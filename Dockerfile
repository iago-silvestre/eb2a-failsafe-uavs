# Use the official PX4 development image with ROS Noetic
FROM px4io/px4-dev-ros-noetic

# Set noninteractive mode for apt
ENV DEBIAN_FRONTEND=noninteractive
# 1. Remove broken shadow-fixed repo BEFORE update
RUN rm -f /etc/apt/sources.list.d/ros-shadow-fixed.list
# 2. safe to update and install tools
RUN apt-get update && apt-get install -y curl gnupg2 lsb-release
# 3. Remove expired GPG key (ignore error if it's already gone)
RUN apt-key del F42ED6FBAB17C654 || true
# 4. Add new ROS GPG key and repo
RUN curl -sSL https://raw.githubusercontent.com/ros/rosdistro/master/ros.asc | apt-key add - && \
    echo "deb http://packages.ros.org/ros/ubuntu focal main" > /etc/apt/sources.list.d/ros-latest.list

# Update package list and install additional dependencies
RUN apt-get update && apt-get install -y \
    git \
	nano \
    wget \
    curl \
    unzip \
	tmux \
    python3-pip \
	python3-vcstool \
    python3-rosinstall \
    python3-rosinstall-generator \
    python3-wstool \
    ros-noetic-rosbridge-suite \
    openjdk-17-jdk \
    x11-xserver-utils \
    && rm -rf /var/lib/apt/lists/*
	
# Install Jason BDI
WORKDIR /root
RUN git clone https://github.com/iago-silvestre/CBSJason ~/jason && \
    cd ~/jason && \
    chmod +x gradlew && \
    ./gradlew config
	
# Install GeographicLib datasets for MAVROS
RUN wget https://raw.githubusercontent.com/mavlink/mavros/master/mavros/scripts/install_geographiclib_datasets.sh \
    && chmod +x install_geographiclib_datasets.sh \
    && ./install_geographiclib_datasets.sh

# Create catkin workspace and clone search-rescue-px4
RUN mkdir -p ~/catkin_ws/src && cd ~/catkin_ws/src && \
    git clone https://github.com/iago-silvestre/eb2a-failsafe-uavs.git

# Build catkin workspace
WORKDIR /root/catkin_ws
RUN /bin/bash -c "source /opt/ros/noetic/setup.bash && catkin_make"

# Update bashrc with required environment variables
RUN echo "source /opt/ros/noetic/setup.bash" >> ~/.bashrc && \
    echo "source ~/catkin_ws/devel/setup.bash" >> ~/.bashrc && \
    echo "export GAZEBO_PLUGIN_PATH=\$GAZEBO_PLUGIN_PATH:/usr/lib/x86_64-linux-gnu/gazebo-11/plugins" >> ~/.bashrc && \
    echo "export ROS_PACKAGE_PATH=\$ROS_PACKAGE_PATH:~/catkin_ws" >> ~/.bashrc && \
    echo "export JASON_HOME=~/jason" >> ~/.bashrc && \
    echo "export PATH=\$JASON_HOME/bin:\$PATH" >> ~/.bashrc && \
    echo "export GAZEBO_MODEL_PATH=\$GAZEBO_MODEL_PATH:~/catkin_ws/src/eb2a-failsafe-uavs/models" >> ~/.bashrc

# Add the stable PPA for MRS UAV System
RUN curl https://ctu-mrs.github.io/ppa-stable/add_ppa.sh | bash || true

# Install the MRS UAV System package
RUN apt-get update && apt-get install -y ros-noetic-mrs-uav-system-full

# Expose the display for GUI-based applications
ENV DISPLAY=:0

# Set working directory and source environment at container startup
WORKDIR /root/catkin_ws/src/eb2a-failsafe-uavs
#ENTRYPOINT ["/bin/bash", "-c", "git pull && source ~/.bashrc && exec bash"]
#ENTRYPOINT ["/bin/bash", "-c", "cp /catkin_ws/src/eb2a-failsafe-uavs/worlds/fireTree.world /opt/ros/noetic/share/mrs_gazebo_common_resources/worlds/ && git -C /catkin_ws/src/eb2a-failsafe-uavs pull && source ~/.bashrc && exec bash"]
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]