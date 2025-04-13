#!/bin/bash

# Copy your world file
cp /catkin_ws/src/eb2a-failsafe-uavs/worlds/fireTree.world /opt/ros/noetic/share/mrs_gazebo_common_resources/worlds/

# Optional: pull latest changes
cd /catkin_ws/src/eb2a-failsafe-uavs && git pull

# Make sure the firefight script is executable
chmod +x /catkin_ws/src/eb2a-failsafe-uavs/tmux/firefight/start.sh

# Source environment
source ~/.bashrc

# Start interactive shell
exec bash
