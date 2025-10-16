#!/usr/bin/env python
import rospy
import time
import random
from std_msgs.msg import Float64, String

class TempFailureTest:
    def __init__(self):
        rospy.init_node('temp_failure_test', anonymous=True)

        self.temp_pub = rospy.Publisher('/uav1/fire_temperature', Float64, queue_size=1)
        self.temp_sev_pub = rospy.Publisher('/uav1/cp1', String, queue_size=1)
        rospy.Subscriber('/agent_detected_failure_uav1', String, self.failure_callback)
        self.fail_det_pub = rospy.Publisher('/agent_detected_failure_uav1', String, queue_size=1)

        self.reaction_received = False
        self.waiting_for_reaction = False
        self.perception_time = None

        self.reaction_times = []

    def publish_temperature(self, value):
        msg = Float64(data=value)
        self.temp_pub.publish(msg)
    
    def publish_temperature_sev(self, value):
        msg = String(data=value)
        self.temp_sev_pub.publish(msg)
        #rospy.loginfo(f"Published temperature: {value}")

    def publish_failure_det(self, value):
        msg = String(data=value)
        self.fail_det_pub.publish(msg)
        #self.temp_sev_pub.publish(msg)

    def failure_callback(self, msg):
        if self.waiting_for_reaction and msg.data.strip() == "1":
            reaction_time = time.perf_counter()
            delay_ms = (reaction_time - self.perception_time) * 1000
            self.reaction_times.append((self.perception_time, reaction_time, delay_ms))
            self.reaction_received = True
            rospy.loginfo(f"Reaction received. Delay: {delay_ms:.2f} ms")
            self.publish_failure_det("0")

    def run(self):
        self.publish_temperature(10.0)
        rospy.sleep(2.0)
        for i in range(20):
            if rospy.is_shutdown():
                break
            self.publish_temperature_sev(str(i))
            self.perception_time = time.perf_counter()

            # Wait for reaction
            self.reaction_received = False
            self.waiting_for_reaction = True
            start = time.perf_counter()
            while not rospy.is_shutdown() and not self.reaction_received and time.perf_counter() - start < 10:
                rospy.sleep(0.01)
            self.waiting_for_reaction = False

            rospy.sleep(random.uniform(2.0, 10.0))

            # Wait briefly before next round
            rospy.sleep(1.0)

        self.save_reaction_times()
        rospy.loginfo("Finished temperature tests.")
        rospy.signal_shutdown("Done")

    def save_reaction_times(self):
        delays = [delay for _, _, delay in self.reaction_times]

        with open("reaction_times.log", "w") as f:
            for p, r, delay in self.reaction_times:
                f.write(f"{p}\t{r}\t{delay:.2f}\n")

            if delays:
                min_delay = min(delays)
                max_delay = max(delays)
                avg_delay = sum(delays) / len(delays)

                f.write(f"\nTotal events: {len(delays)}\n")
                f.write(f"Min delay: {min_delay:.2f} ms\n")
                f.write(f"Max delay: {max_delay:.2f} ms\n")
                f.write(f"Avg delay: {avg_delay:.2f} ms\n")

                rospy.loginfo(
                    f"Reaction stats -> Min: {min_delay:.2f} ms, "
                    f"Max: {max_delay:.2f} ms, Avg: {avg_delay:.2f} ms"
                )
            else:
                f.write("No reaction times recorded.\n")
                rospy.loginfo("No reaction times recorded.")

if __name__ == '__main__':
    try:
        node = TempFailureTest()
        node.run()
    except rospy.ROSInterruptException:
        pass
