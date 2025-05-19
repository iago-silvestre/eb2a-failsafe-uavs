#!/usr/bin/env python
import rospy
import time
from std_msgs.msg import Float64, String

class TempFailureTest:
    def __init__(self):
        rospy.init_node('temp_failure_test', anonymous=True)

        self.temp_pub = rospy.Publisher('/uav1/fire_temperature', Float64, queue_size=1)
        rospy.Subscriber('/agent_detected_failure_uav1', String, self.failure_callback)

        self.reaction_received = False
        self.waiting_for_reaction = False
        self.perception_time = None

        self.reaction_times = []

    def publish_temperature(self, value):
        msg = Float64(data=value)
        self.temp_pub.publish(msg)
        rospy.loginfo(f"Published temperature: {value}")

    def failure_callback(self, msg):
        if self.waiting_for_reaction and msg.data.strip() == "1":
            reaction_time = time.perf_counter()
            delay_ms = (reaction_time - self.perception_time) * 1000
            self.reaction_times.append((self.perception_time, reaction_time, delay_ms))
            self.reaction_received = True
            rospy.loginfo(f"Reaction received. Delay: {delay_ms:.2f} ms")

    def run(self):
        for i in range(10):
            if rospy.is_shutdown():
                break

            # Publish 80.0
            self.publish_temperature(80.0)
            self.perception_time = time.perf_counter()

            # Wait for reaction
            self.reaction_received = False
            self.waiting_for_reaction = True
            start = time.perf_counter()
            while not rospy.is_shutdown() and not self.reaction_received and time.perf_counter() - start < 10:
                rospy.sleep(0.1)
            self.waiting_for_reaction = False

            # Publish 0.0 to reset
            self.publish_temperature(0.0)

            # Wait briefly before next round
            rospy.sleep(1.0)

        self.save_reaction_times()
        rospy.loginfo("Finished temperature tests.")
        rospy.signal_shutdown("Done")

    def save_reaction_times(self):
        with open("reaction_times.log", "w") as f:
            for p, r, delay in self.reaction_times:
                f.write(f"{p}\t{r}\t{delay:.2f}\n")
            f.write(f"Total events: {len(self.reaction_times)}\n")

if __name__ == '__main__':
    try:
        node = TempFailureTest()
        node.run()
    except rospy.ROSInterruptException:
        pass
