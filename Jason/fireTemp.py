#!/usr/bin/env python

import rospy
import random
import numpy as np
import math
from gazebo_msgs.srv import DeleteModel, GetModelState
from geometry_msgs.msg import Pose
from std_msgs.msg import Int32, Float64, Int8, String
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

        self.last_odom_time = rospy.Time.now()
        self.comm_failure = False
        self.start_time = None

        self.bridge = CvBridge()

        self.in_danger_zone = False
        self.danger_start_time = None
        self.max_temp_in_zone = float('-inf')  # or 0.0 if you prefer
        self.temp_sum = 0.0
        self.temp_count = 0
        self.temp_above_50 = False
        self.temp_timer_start = None

        self.failure_sub = rospy.Subscriber('/agent_detected_failure_uav1', String, self.failure_callback)


        self.fire_size_pub = rospy.Publisher("fireSize", Int8, queue_size=1)
        self.subscriber_del = rospy.Subscriber('/fightFire', Int32, self.del_callback)

        self.path_pub = rospy.Publisher('/uav1_lastWP', Int8, queue_size=1)
        self.battery_pub = rospy.Publisher('/battery_uav1', Float64, queue_size=1)
        self.fire_detection_pub = rospy.Publisher('/uav1/fire_detection', Int32, queue_size=1)
        self.temp_pub = rospy.Publisher('/uav1/fire_temperature', Float64, queue_size=1)
        self.failure_pub = rospy.Publisher('/uav1/failure', Int8, queue_size=1)

        self.fire_direction_pub = rospy.Publisher("/uav1/fire_direction", String, queue_size=1)
        self.oppos_fire_dir_pub = rospy.Publisher("/uav1/oppos_fire_dir", String, queue_size=1)
        self.time_pub = rospy.Publisher('/uav1/time_since_comm_failure', Float64, queue_size=1)


        rospy.Subscriber('/uav1/trajectory_generation/path', Path, self.path_callback)
        rospy.Subscriber('/uav1/ground_truth', Odometry, self.odom_callback)
        rospy.Subscriber('/uav1/bluefox_optflow/image_raw', Image, self.image_callback)
        rospy.Subscriber('/recharge_battery', Int8, self.recharge_battery_callback)

        rospy.Subscriber('/uav1/comm_failure', Int32, self.comm_failure_callback)
        self.publish_timer = rospy.Timer(rospy.Duration(0.5), self.publish_elapsed_time)

        rospy.wait_for_service('/gazebo/get_model_state')
        self.get_model_state = rospy.ServiceProxy('/gazebo/get_model_state', GetModelState)

        rospy.Timer(rospy.Duration(1), self.update_battery)
        # Immediately publish failure = 1
        rospy.sleep(0.5)  # Short delay to ensure publisher is registered
        #self.failure_pub.publish(Int8(data=1))
        #rospy.loginfo("[DEBUG] Published failure message: 1")
        
        rospy.wait_for_service('/gazebo/get_model_state')
        self.get_model_state = rospy.ServiceProxy('/gazebo/get_model_state', GetModelState)

        try:
            model_state = self.get_model_state("tree_red", "")
            self.tree_x = model_state.pose.position.x
            self.tree_y = model_state.pose.position.y
            self.tree_z = 5.0
            rospy.loginfo(f"[DEBUG] Tree position initialized at ({self.tree_x}, {self.tree_y})")
        except rospy.ServiceException as e:
            rospy.logerr(f"[ERROR] Failed to get initial tree position: {e}")
            self.tree_x = 0.0
            self.tree_y = 0.0
            self.tree_z = 0.0

    def comm_failure_callback(self, msg):
        if msg.data == 1 and not self.comm_failure:
            self.comm_failure = True
            self.start_time = rospy.Time.now()
            rospy.loginfo("Comm failure started.")
        elif msg.data == 0 and self.comm_failure:
            self.comm_failure = False
            self.start_time = None
            rospy.loginfo("Comm failure cleared. Timer reset.")
            self.time_pub.publish(0.0)  # Optional: reset value
    
    def publish_elapsed_time(self, event):
        if self.comm_failure and self.start_time is not None:
            elapsed = (rospy.Time.now() - self.start_time).to_sec()
            self.time_pub.publish(Float64(data=elapsed))

    def path_callback(self, msg):
        self.waypoints = [(pt.position.x, pt.position.y) for pt in msg.points]
        #rospy.loginfo(f"[DEBUG] Received path with {len(self.waypoints)} waypoints:")
        #for i, (x, y) in enumerate(self.waypoints):
        #    rospy.loginfo(f"  Waypoint {i}: ({x:.2f}, {y:.2f})")
        self.last_waypoint_index = 0
        self.path_pub.publish(0)

    def angle_to_direction(self, angle_deg):
        """Convert angle in degrees to compass direction string."""
        directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW']
        idx = int(((angle_deg + 22.5) % 360) / 45)
        return directions[idx]

    def odom_callback(self, msg):
        current_time = rospy.Time.now()
        if (current_time - self.last_odom_time).to_sec() < 0.1:
            return  # Skip this message

        self.last_odom_time = current_time
        x = msg.pose.pose.position.x
        y = msg.pose.pose.position.y
        z = msg.pose.pose.position.z 

        FIRE_RADIUS = 1.0
        DANGER_RADIUS = 7.0

        fire_distance = ((x - self.tree_x) ** 2 +
                 (y - self.tree_y) ** 2 +
                 (z - 5) ** 2) ** 0.5

        distance_from_edge = max(0.0, fire_distance - FIRE_RADIUS)
        temperature = max(0.0, 100.0 - distance_from_edge * 10.0)

        #print(f"fire_distance : {fire_distance:.4f}")
        #print(f"distance_from_edge : {distance_from_edge:.4f}")
        #print(f"Temp: {temperature:.2f} ")
         # Start timer when temperature > 50 if not already started
        self.temp_pub.publish(temperature)
        if temperature > 50.0 and not self.temp_above_50:
            self.temp_above_50 = True
            self.temp_timer_start = rospy.Time.now()
        
        # Optionally, reset timer if temp goes back below 50 (if you want that logic)
        if temperature <= 50.0 and self.temp_above_50:
            self.temp_above_50 = False
            self.temp_timer_start = None

        if fire_distance <= DANGER_RADIUS:
            if not self.in_danger_zone:
                self.in_danger_zone = True
                print("[Danger Zone Enter]")
                self.danger_start_time = rospy.Time.now()
                self.temp_sum = 0.0
                self.temp_count = 0
                self.max_temp_in_zone = float('-inf')  # reset max temp

            self.temp_sum += temperature
            self.temp_count += 1
            if temperature > self.max_temp_in_zone:
                self.max_temp_in_zone = temperature

        else:
            if self.in_danger_zone:
                self.in_danger_zone = False
                avg_temp = self.temp_sum / self.temp_count if self.temp_count > 0 else 0.0
                elapsed = (rospy.Time.now() - self.danger_start_time).to_sec()
                print(f"[Danger Zone Exit] Time inside: {elapsed:.4f} sec, "
                    f"Avg temperature: {avg_temp:.2f}, "
                    f"Max temperature: {self.max_temp_in_zone:.2f}")

        

        # If temperature is too low, publish "none"
        if temperature < 10.0:
            msg = String()
            msg.data = "none"
            self.fire_direction_pub.publish(msg)
        else:
            # Calculate angle from UAV to fire in radians
            dx = self.tree_x - x
            dy = self.tree_y - y
            angle_rad = math.atan2(dy, dx)  # Bearing in radians
            angle_deg = math.degrees(angle_rad)  # Optional: convert to degrees
            directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW']
            oppositeD =  ['S', 'SW', 'W', 'NW', 'N', 'NE', 'E', 'SE']
            idx = int(((angle_deg + 22.5) % 360) / 45)
            fire_direction_str = directions[idx]
            oppos_fire_dir_str = oppositeD[idx]
            #fire_direction_str = self.angle_to_direction(angle_deg)

            msg = String()
            msg.data = fire_direction_str
            self.fire_direction_pub.publish(msg)

            msg2 = String()
            msg2.data = oppos_fire_dir_str
            self.oppos_fire_dir_pub.publish(msg2)

        # Waypoint tracking
        for i, (wp_x, wp_y) in enumerate(self.waypoints):
            distance = ((x - wp_x) ** 2 + (y - wp_y) ** 2) ** 0.5
            if distance < self.threshold:
                self.last_waypoint_index = i + 1
                self.path_pub.publish(i + 1)
                break
        
    def failure_callback(self, msg):
        if msg.data == '1' and self.temp_above_50:
            elapsed = (rospy.Time.now() - self.temp_timer_start).to_sec() if self.temp_timer_start else 0.0
            print(f"[Failure detected] Timer stopped after {elapsed:.4f} seconds with temperature > 50.")
            self.temp_above_50 = False
            self.temp_timer_start = None

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
        #self.battery_pub.publish(self.battery_level)
        self.battery_pub.publish(100.0)
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
