#include <ros/ros.h>
#include <std_msgs/Float64.h>
#include <std_msgs/String.h>

#include <chrono>
#include <random>
#include <string>
#include <vector>
#include <fstream>
#include <numeric>
#include <cmath>

struct ReactionTime {
    int msg_idx;
    double delay_perf;
    double delay_mono;
    double delay_ns;
};

class TempFailureTest {
public:
    TempFailureTest() : gen(rd()), dist(0.0, 1.0) {
        ros::NodeHandle nh;
        temp_pub = nh.advertise<std_msgs::Float64>("/uav1/fire_temperature", 1);
        sev_pub  = nh.advertise<std_msgs::String>("/uav1/cp0", 1);
        sub      = nh.subscribe("/agent_detected_failure_uav1", 1, &TempFailureTest::failureCallback, this);

        total_messages = 1000;
        reaction_received = false;
        current_msg = -1;
    }

    void run() {
        publishTemperature(10.0);
        ros::Duration(2.0).sleep();

        ros::Rate rate(1000); // 1000 Hz
        int i = 0;
        while (i < total_messages && ros::ok()) {
            if (dist(gen) < 0.01) {  // 1% chance
                current_msg = i;
                reaction_received = false;

                // Start timers
                perception_perf = std::chrono::high_resolution_clock::now();
                perception_mono = std::chrono::steady_clock::now();
                perception_ns   = std::chrono::system_clock::now();

                publishSeverity(std::to_string(i));

                auto start_wait = std::chrono::high_resolution_clock::now();
                double timeout = 10.0; // seconds

                while (ros::ok() && !reaction_received) {
                    auto elapsed = std::chrono::duration<double>(std::chrono::high_resolution_clock::now() - start_wait).count();
                    if (elapsed > timeout) {
                        ROS_WARN_STREAM("No reaction for message " << i << " within " << timeout << "s");
                        break;
                    }
                    ros::spinOnce();
                    rate.sleep();
                }
                i++; // increment only if published
            }
            ros::spinOnce();
            rate.sleep();
        }

        saveReactionTimes();
        ROS_INFO("Finished tests.");
    }

private:
    ros::Publisher temp_pub;
    ros::Publisher sev_pub;
    ros::Subscriber sub;

    std::vector<ReactionTime> reaction_times;

    std::chrono::time_point<std::chrono::high_resolution_clock> perception_perf;
    std::chrono::time_point<std::chrono::steady_clock> perception_mono;
    std::chrono::time_point<std::chrono::system_clock> perception_ns;

    bool reaction_received;
    int current_msg;
    int total_messages;

    std::random_device rd;
    std::mt19937 gen;
    std::uniform_real_distribution<> dist;

    void publishTemperature(double value) {
        std_msgs::Float64 msg;
        msg.data = value;
        temp_pub.publish(msg);
    }

    void publishSeverity(const std::string& value) {
        std_msgs::String msg;
        msg.data = value;
        sev_pub.publish(msg);
        ROS_INFO_STREAM("Published message " << value);
    }

    void failureCallback(const std_msgs::String::ConstPtr& msg) {
        if (current_msg >= 0 && !reaction_received) {
            if (msg->data == std::to_string(current_msg)) {
                // Compute delays
                auto now_perf = std::chrono::high_resolution_clock::now();
                auto now_mono = std::chrono::steady_clock::now();
                auto now_ns   = std::chrono::system_clock::now();

                double delay_perf = std::chrono::duration<double, std::milli>(now_perf - perception_perf).count();
                double delay_mono = std::chrono::duration<double, std::milli>(now_mono - perception_mono).count();
                double delay_ns   = std::chrono::duration<double, std::milli>(now_ns - perception_ns).count();

                reaction_times.push_back({current_msg, delay_perf, delay_mono, delay_ns});
                reaction_received = true;

                ROS_INFO_STREAM("Reaction received for message " << current_msg
                                << " | perf: " << delay_perf << " ms, "
                                << "mono: " << delay_mono << " ms, "
                                << "ns: " << delay_ns << " ms");
            }
        }
    }

    void saveReactionTimes() {
        std::ofstream file("reaction_times.log");
        file << "msg_idx\tperf_ms\tmonotonic_ms\ttime_ns_ms\n";
        for (auto& rt : reaction_times) {
            file << rt.msg_idx << "\t"
                 << rt.delay_perf << "\t"
                 << rt.delay_mono << "\t"
                 << rt.delay_ns << "\n";
        }
        file.close();

        if (!reaction_times.empty()) {
            std::vector<double> perf_vals, mono_vals, ns_vals;
            for (auto& rt : reaction_times) {
                perf_vals.push_back(rt.delay_perf);
                mono_vals.push_back(rt.delay_mono);
                ns_vals.push_back(rt.delay_ns);
            }
            summary("PerfCounter", perf_vals);
            summary("Monotonic", mono_vals);
            summary("TimeNS", ns_vals);
        }
    }

    void summary(const std::string& name, const std::vector<double>& vals) {
        if (vals.empty()) return;
        auto minv = *std::min_element(vals.begin(), vals.end());
        auto maxv = *std::max_element(vals.begin(), vals.end());
        double avg = std::accumulate(vals.begin(), vals.end(), 0.0) / vals.size();

        double sumsq = 0.0;
        for (auto v : vals) sumsq += (v - avg) * (v - avg);
        double stdev = (vals.size() > 1) ? std::sqrt(sumsq / (vals.size()-1)) : 0.0;

        ROS_INFO_STREAM(name << " -> min: " << minv << " ms, "
                             << "max: " << maxv << " ms, "
                             << "avg: " << avg << " ms, "
                             << "std: " << stdev << " ms");
    }
};

int main(int argc, char** argv) {
    ros::init(argc, argv, "temp_failure_test");
    TempFailureTest node;
    node.run();
    return 0;
}

