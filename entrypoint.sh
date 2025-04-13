#!/bin/bash

# Optional: pull latest changes
cd /root/catkin_ws/src/eb2a-failsafe-uavs && git pull

# Make sure the firefight script is executable
chmod +x /root/catkin_ws/src/eb2a-failsafe-uavs/tmux/firefight/start.sh

# Copy your world file
cp /root/catkin_ws/src/eb2a-failsafe-uavs/worlds/fireTree.world /opt/ros/noetic/share/mrs_gazebo_common_resources/worlds/

# Source environment
source ~/.bashrc

# Start interactive shell
exec bash
