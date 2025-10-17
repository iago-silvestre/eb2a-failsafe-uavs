#!/usr/bin/env python3
import rospy
import time
import statistics
import numpy as np
from std_msgs.msg import Float64, String

class TempFailureTest:
    def __init__(self):
        rospy.init_node('temp_failure_test', anonymous=True)

        # Publishers and subscriber
        self.temp_pub = rospy.Publisher('/uav1/fire_temperature', Float64, queue_size=1)
        self.sev_pub = rospy.Publisher('/uav1/cp0', String, queue_size=1)
        rospy.Subscriber('/agent_detected_failure_uav1', String, self.failure_callback)

        # Reaction tracking
        self.reaction_times = []  # stores tuples (i, perf_counter_ms, monotonic_ms, time_ns_ms)
        self.perception_perf = None
        self.perception_mono = None
        self.perception_ns = None
        self.reaction_received = False
        self.current_msg = None

        self.total_messages = 100

    def publish_temperature(self, value):
        self.temp_pub.publish(Float64(data=value))

    def publish_severity(self, value):
        self.sev_pub.publish(String(data=value))
        rospy.loginfo(f"Published message {value}")

    def failure_callback(self, msg):
        if self.current_msg is not None and not self.reaction_received:
            if msg.data.strip() == str(self.current_msg):
                # Compute delays using all three timers
                delay_perf = (time.perf_counter() - self.perception_perf) * 1000.0
                delay_mono = (time.monotonic() - self.perception_mono) * 1000.0
                delay_ns = (time.time_ns() - self.perception_ns) / 1e6

                self.reaction_times.append(
                    (self.current_msg, delay_perf, delay_mono, delay_ns)
                )
                self.reaction_received = True
                rospy.loginfo(
                    f"Reaction received for message {self.current_msg} | "
                    f"perf: {delay_perf:.2f} ms, mono: {delay_mono:.2f} ms, ns: {delay_ns:.2f} ms"
                )

    def run(self):
        # Initial temperature
        self.publish_temperature(10.0)
        rospy.sleep(2.0)

        rate = rospy.Rate(1000)  # run at 1000 Hz
        i = 0
        while i < self.total_messages and not rospy.is_shutdown():
            # With small probability, publish a message
            if np.random.rand() < 0.001:  # 1% chance per loop iteration
                self.current_msg = i
                self.reaction_received = False

                # Start all three timers
                self.perception_perf = time.perf_counter()
                self.perception_mono = time.monotonic()
                self.perception_ns = time.time_ns()

                self.publish_severity(str(i))

                # Wait for reaction or timeout
                start_wait = time.perf_counter()
                timeout = 10.0  # seconds
                while (not rospy.is_shutdown() and 
                    not self.reaction_received and 
                    (time.perf_counter() - start_wait < timeout)):
                    rate.sleep()

                if not self.reaction_received:
                    rospy.logwarn(f"No reaction for message {i} within {timeout}s")

                i += 1  # only increment when we actually publish

            # If no publish, just keep looping
            rate.sleep()

        self.save_reaction_times()
        rospy.loginfo("Finished tests.")
        rospy.signal_shutdown("Done")

    def save_reaction_times(self):
        with open("reaction_times.log", "w") as f:
            f.write("msg_idx\tperf_ms\tmonotonic_ms\ttime_ns_ms\n")
            for idx, perf, mono, ns in self.reaction_times:
                f.write(f"{idx}\t{perf:.2f}\t{mono:.2f}\t{ns:.2f}\n")

        # Summary statistics for each timer
        if self.reaction_times:
            perf_vals = [t[1] for t in self.reaction_times]
            mono_vals = [t[2] for t in self.reaction_times]
            ns_vals = [t[3] for t in self.reaction_times]

            def summary(name, vals):
                rospy.loginfo(
                    f"{name:<11} -> min: {min(vals):.2f} ms, "
                    f"max: {max(vals):.2f} ms, "
                    f"avg: {sum(vals)/len(vals):.2f} ms, "
                    f"std: {statistics.stdev(vals):.2f} ms"
                )

            summary("PerfCounter", perf_vals)
            summary("Monotonic", mono_vals)
            summary("TimeNS", ns_vals)

if __name__ == "__main__":
    try:
        node = TempFailureTest()
        node.run()
    except rospy.ROSInterruptException:
        pass
