#!/usr/bin/env python

import rospy
import random
import numpy as np
from gazebo_msgs.srv import DeleteModel, GetModelState
from geometry_msgs.msg import Pose
from std_msgs.msg import Int32, Float64, Int8
from nav_msgs.msg import Odometry
from mrs_msgs.srv import PathSrv,PathSrvRequest
from mrs_msgs.msg import Reference,Path
from sensor_msgs.msg import Image
from cv_bridge import CvBridge, CvBridgeError
import cv2

class FireTempNode:
    def __init__(self):
        rospy.init_node('fireTemp', anonymous=True)

        self.threshold = 1.0
        self.fireSize = 4
        self.count = 0
        self.auxcount = 0
        self.teste = 0
        self.battery_level = 100.0
        self.waypoints = []
        self.last_waypoint_index = 0

        self.bridge = CvBridge()

        self.fire_size_pub = rospy.Publisher("fireSize", Int8, queue_size=1)
        self.subscriber_del = rospy.Subscriber('/fightFire', Int32, self.del_callback)

        self.path_pub = rospy.Publisher('/uav1_lastWP', Int8, queue_size=1)
        self.battery_pub = rospy.Publisher('/battery_uav1', Float64, queue_size=1)
        self.fire_detection_pub = rospy.Publisher('/uav1/fire_detection', Int32, queue_size=1)
        self.temp_pub = rospy.Publisher('/uav1/fire_temperature', Float64, queue_size=1)
        self.failure_pub = rospy.Publisher('/uav1/failure', Int8, queue_size=1)

        rospy.Subscriber('/uav1/trajectory_generation/path', Path, self.path_callback)
        rospy.Subscriber('/uav1/ground_truth', Odometry, self.odom_callback)
        rospy.Subscriber('/uav1/bluefox_optflow/image_raw', Image, self.image_callback)
        rospy.Subscriber('/recharge_battery', Int8, self.recharge_battery_callback)

        rospy.wait_for_service('/gazebo/get_model_state')
        self.get_model_state = rospy.ServiceProxy('/gazebo/get_model_state', GetModelState)

        rospy.Timer(rospy.Duration(1), self.update_battery)
        # Immediately publish failure = 1
        rospy.sleep(0.5)  # Short delay to ensure publisher is registered
        self.failure_pub.publish(Int8(data=1))
        rospy.loginfo("[DEBUG] Published failure message: 1")
        
        rospy.wait_for_service('/gazebo/get_model_state')
        self.get_model_state = rospy.ServiceProxy('/gazebo/get_model_state', GetModelState)

        try:
            model_state = self.get_model_state("tree_red", "")
            self.tree_x = model_state.pose.position.x
            self.tree_y = model_state.pose.position.y
            rospy.loginfo(f"[DEBUG] Tree position initialized at ({self.tree_x}, {self.tree_y})")
        except rospy.ServiceException as e:
            rospy.logerr(f"[ERROR] Failed to get initial tree position: {e}")
            self.tree_x = 0.0
            self.tree_y = 0.0

    def path_callback(self, msg):
        self.waypoints = [(pt.position.x, pt.position.y) for pt in msg.points]
        rospy.loginfo(f"[DEBUG] Received path with {len(self.waypoints)} waypoints:")
        for i, (x, y) in enumerate(self.waypoints):
            rospy.loginfo(f"  Waypoint {i}: ({x:.2f}, {y:.2f})")
        self.last_waypoint_index = 0
        self.path_pub.publish(0)


    def odom_callback(self, msg):
        x = msg.pose.pose.position.x
        y = msg.pose.pose.position.y

        fire_distance = ((x - self.tree_x) ** 2 + (y - self.tree_y) ** 2) ** 0.5
        temperature = max(0.0, 100.0 - fire_distance * 10.0)  # basic decay model
        self.temp_pub.publish(temperature)
        for i, (wp_x, wp_y) in enumerate(self.waypoints):
            distance = ((x - wp_x) ** 2 + (y - wp_y) ** 2) ** 0.5
            rospy.loginfo(f"[DEBUG] Checking waypoint {i}: ({wp_x}, {wp_y}) | Distance: {distance:.2f}")
            if distance < self.threshold:
                self.last_waypoint_index = i + 1
                self.path_pub.publish(i + 1)
                break
        #if not self.waypoints:
        #    return

        # Waypoint tracking
        

    def image_callback(self, msg):
        try:
            cv_image = self.bridge.imgmsg_to_cv2(msg, "rgb8")
        except CvBridgeError as e:
            rospy.logerr("CvBridge Error: %s", e)
            return

        lower_red = np.array([150, 0, 0])
        upper_red = np.array([255, 100, 100])
        red_mask = cv2.inRange(cv_image, lower_red, upper_red)
        red_pixel_count = np.sum(red_mask == 255)
        disableFireDetect = 0
        #self.fire_detection_pub.publish(red_pixel_count)
        self.fire_detection_pub.publish(disableFireDetect)

    def update_battery(self, event):
        self.battery_level -= 0.1
        self.battery_pub.publish(self.battery_level)
        self.fire_size_pub.publish(self.fireSize)

    def recharge_battery_callback(self, msg):
        if msg.data == 1:
            self.battery_level = 100.0
            self.battery_pub.publish(self.battery_level)
            rospy.loginfo("Recharged battery of UAV1 to 100")

    def del_callback(self, msg):
        if msg.data != 0:
            model_name = "tree_red_" + str(self.count)
            rospy.loginfo("Deleting model: %s", model_name)
            self.auxcount += 1
            probability = random.random()
            if self.auxcount != -2:
                if probability <= 0.75:
                    try:
                        self.count += 1
                        self.fireSize -= 1
                        delete_model = rospy.ServiceProxy('/gazebo/delete_model', DeleteModel)
                        response = delete_model(model_name)
                        rospy.loginfo("Model deletion response: %s", response.status_message)
                        self.fire_size_pub.publish(self.fireSize)
                    except rospy.ServiceException as e:
                        rospy.logerr("Service call failed: %s", e)
            self.teste += 1

if __name__ == '__main__':
    try:
        FireTempNode()
        rospy.spin()
    except rospy.ROSInterruptException:
        pass
