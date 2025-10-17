#!/usr/bin/env python3
import rospy
import time
import statistics
import numpy as np
from std_msgs.msg import Float64, String

class CriticalEventTest:
    def __init__(self):
        rospy.init_node('critical_event_test', anonymous=True)

        # Publishers and subscriber
        self.temp_pub = rospy.Publisher('/uav1/fire_temperature', Float64, queue_size=1)
        self.event_pub = rospy.Publisher('/uav1/cp0', String, queue_size=1)
        rospy.Subscriber('/agent_detected_failure_uav1', String, self.failure_callback)

        # Reaction tracking
        self.reaction_times = []
        self.event_timestamp = None
        self.current_msg = None
        self.reaction_received = False

        self.total_events = 50  # number of critical events to simulate

    def publish_temperature(self, value):
        self.temp_pub.publish(Float64(data=value))

    def publish_event(self, value):
        self.event_pub.publish(String(data=value))
        rospy.loginfo(f"Event published: {value}")

    def failure_callback(self, msg):
        if self.current_msg is not None and not self.reaction_received:
            if msg.data.strip() == str(self.current_msg):
                reaction_time_ms = (time.perf_counter() - self.event_timestamp) * 1000.0
                self.reaction_times.append(reaction_time_ms)
                self.reaction_received = True
                rospy.loginfo(f"Reaction received for event {self.current_msg}. Delay: {reaction_time_ms:.2f} ms")

    def run(self):
        self.publish_temperature(10.0)
        rospy.sleep(2.0)

        for i in range(self.total_events):
            if rospy.is_shutdown():
                break

            # Prepare for reaction
            self.current_msg = i
            self.reaction_received = False
            self.event_timestamp = time.perf_counter()

            # Publish the event
            self.publish_event(str(i))

            # Wait for reaction or timeout
            timeout = 10.0
            start_wait = time.perf_counter()
            rate = rospy.Rate(1000)  # 1000 Hz spin
            while not rospy.is_shutdown() and not self.reaction_received and (time.perf_counter() - start_wait < timeout):
                rate.sleep()

            if not self.reaction_received:
                rospy.logwarn(f"No reaction for event {i} within {timeout}s")

            # Random interval until next event (simulate unpredictable critical events)
            interval = np.clip(np.random.normal(loc=3.0, scale=1.0), 1.0, 5.0)
            interval_start = time.perf_counter()
            while not rospy.is_shutdown() and (time.perf_counter() - interval_start < interval):
                rate.sleep()

        self.save_reaction_times()
        rospy.loginfo("Finished critical event simulation.")
        rospy.signal_shutdown("Done")

    def save_reaction_times(self):
        with open("reaction_times.log", "w") as f:
            for idx, delay in enumerate(self.reaction_times):
                f.write(f"{idx}\t{delay:.2f}\n")

        if self.reaction_times:
            min_delay = min(self.reaction_times)
            max_delay = max(self.reaction_times)
            avg_delay = sum(self.reaction_times) / len(self.reaction_times)
            std_delay = statistics.stdev(self.reaction_times) if len(self.reaction_times) > 1 else 0.0

            rospy.loginfo(
                f"Reaction stats -> Min: {min_delay:.2f} ms, Max: {max_delay:.2f} ms, "
                f"Avg: {avg_delay:.2f} ms, Std: {std_delay:.2f} ms"
            )
        else:
            rospy.loginfo("No reactions recorded.")

if __name__ == "__main__":
    try:
        node = CriticalEventTest()
        node.run()
    except rospy.ROSInterruptException:
        pass
