#include <ros/ros.h>
#include <std_msgs/String.h>
#include <std_msgs/Float64.h>
#include <chrono>
#include <vector>
#include <random>
#include <fstream>
#include <numeric>
#include <cmath>
#include <algorithm>

class TempFailureTest {
public:
    TempFailureTest(ros::NodeHandle& nh)
        : nh_(nh), reaction_received_(false), current_msg_(0), total_messages_(101) {  // 101 iterations

        temp_pub_ = nh_.advertise<std_msgs::Float64>("/uav1/fire_temperature", 1);
        sev_pub_ = nh_.advertise<std_msgs::String>("/uav1/cp2", 1);
        sub_ = nh_.subscribe("/agent_detected_failure_uav1", 1,
                             &TempFailureTest::failureCallback, this);

        rng_.seed(std::random_device()());
    }

    void run() {
        publishTemperature(10.0);
        ros::Duration(2.0).sleep();

        for (int i = 0; i < total_messages_ && ros::ok(); ++i) {
            current_msg_ = i;
            reaction_received_ = false;

            perception_time_ = std::chrono::high_resolution_clock::now();
            publishSeverity(std::to_string(i));

            // Wait for reaction or timeout
            auto wait_start = std::chrono::high_resolution_clock::now();
            double timeout = 10.0; // seconds
            while (ros::ok() && !reaction_received_ &&
                   std::chrono::duration<double>(std::chrono::high_resolution_clock::now() - wait_start).count() < timeout) {
                ros::spinOnce();
            }

            if (!reaction_received_) {
                ROS_WARN("No reaction for message %d within %.1f s", i, timeout);
            }

            // Fixed interval before next message
            //double interval = 2.0; // change here if needed
            std::uniform_real_distribution<double> dist(2.0, 5.0);
	    double interval = dist(rng_);
            auto interval_start = std::chrono::high_resolution_clock::now();
            while (ros::ok() &&
                   std::chrono::duration<double>(std::chrono::high_resolution_clock::now() - interval_start).count() < interval) {
                ros::spinOnce();
            }
        }

        saveReactionTimes();
        ROS_INFO("Finished tests.");
    }

private:
    ros::NodeHandle nh_;
    ros::Publisher temp_pub_;
    ros::Publisher sev_pub_;
    ros::Subscriber sub_;

    bool reaction_received_;
    int current_msg_;
    int total_messages_;
    std::chrono::high_resolution_clock::time_point perception_time_;
    std::vector<double> reaction_times_;
    std::mt19937 rng_;

    void publishTemperature(double value) {
        std_msgs::Float64 msg;
        msg.data = value;
        temp_pub_.publish(msg);
        ROS_INFO("Published temperature %.2f", value);
    }

    void publishSeverity(const std::string& value) {
        std_msgs::String msg;
        msg.data = value;
        sev_pub_.publish(msg);
        ROS_INFO("Published message %s", value.c_str());
    }

    void failureCallback(const std_msgs::String::ConstPtr& msg) {
        if (!reaction_received_ && msg->data == std::to_string(current_msg_)) {
            auto now = std::chrono::high_resolution_clock::now();
            double delay_ms = std::chrono::duration<double, std::milli>(now - perception_time_).count();
            reaction_times_.push_back(delay_ms);
            reaction_received_ = true;
            ROS_INFO("Reaction received for message %d. Delay: %.2f ms", current_msg_, delay_ms);
        }
    }

    void saveReactionTimes() {
        std::ofstream file("reaction_times.log");
        for (size_t i = 0; i < reaction_times_.size(); ++i) {
            file << i << "\t" << reaction_times_[i] << "\n";
        }

        if (reaction_times_.size() > 1) { // ignore first
            std::vector<double> usable(reaction_times_.begin() + 1, reaction_times_.end());

            double sum = std::accumulate(usable.begin(), usable.end(), 0.0);
            double avg = sum / usable.size();
            double min_val = *std::min_element(usable.begin(), usable.end());
            double max_val = *std::max_element(usable.begin(), usable.end());
            double sq_sum = std::inner_product(usable.begin(), usable.end(),
                                              usable.begin(), 0.0);
            double stddev = std::sqrt(sq_sum / usable.size() - avg * avg);

            ROS_INFO("Reaction stats (first ignored) -> Min: %.2f ms, Max: %.2f ms, Avg: %.2f ms, Std: %.2f ms",
                     min_val, max_val, avg, stddev);
        } else {
            ROS_WARN("Not enough reactions to calculate stats.");
        }

        file.close();
        ROS_INFO("Saved reaction times to reaction_times.log");
    }
};

int main(int argc, char** argv) {
    ros::init(argc, argv, "temp_failure_test");
    ros::NodeHandle nh;

    TempFailureTest tester(nh);
    tester.run();

    return 0;
}

