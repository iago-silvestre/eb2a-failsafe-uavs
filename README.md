# EB2A-Based Fail-Safe Framework for Autonomous UAVs

This repository hosts the source code, simulation setups, and planning resources for a research project focused on improving the safety and responsiveness of autonomous UAVs using the **Expedited Belief-Desire-Intention Agent Architecture (EB2A)**.

Developed as part of a PhD project at the Federal University of Santa Catarina (UFSC), the framework introduces deterministic fail-safe mechanisms and runtime verification strategies for mission-critical scenarios such as forest firefighting.

---

##  Project Overview

Traditional BDI agent architectures often suffer from slow response times in real-time UAV operations. This project proposes a faster, more robust alternative—**EB2A**—enhanced with:

- Deterministic fail-safe procedures
- Failure severity perception
- Runtime monitoring
- Optional integration with PX4/Ardupilot failsafes
- User interface

The goal is to improve agent responsiveness and decision-making in safety-critical UAV missions without relying solely on preconfigured flight stack behaviors.

---
## **Installation Setup (With Docker)**
You can run this setup using Docker. For [Windows](https://docs.docker.com/desktop/setup/install/windows-install/) it is recommended to have [WSL](https://learn.microsoft.com/en-us/windows/wsl/install) installed.
1. **Clone the repository**:
   ```bash
   git clone https://github.com/iago-silvestre/search-rescue-px4.git
   cd search-rescue-px4
   ```

2. **Build the Docker image**:
   ```bash
   docker build -t eb2a .
   ```
---

### **For Windows Users (with VCXsrv)**

1. **Install VCXsrv** from [here](https://github.com/marchaesen/vcxsrv) and start it with XLaunch using default settings.
   
2. **Set the DISPLAY environment**:
Set the `DISPLAY` to point to the host machine’s X11 server:
     ```bash
     set DISPLAY=host.docker.internal:0
     ```

3. **Run the container**:
   ```bash
   docker run -it --rm --env DISPLAY=host.docker.internal:0 --volume /tmp/.X11-unix:/tmp/.X11-unix --env QT_X11_NO_MITSHM=1 --net host eb2a
   ```

---

### **For Linux Users**

1. **Allow Docker to access the X11 server**:
   ```bash
   xhost +local:docker
   ```

2. **Run the container**:
   ```bash
   docker run -it --rm --env DISPLAY=$DISPLAY --volume /tmp/.X11-unix:/tmp/.X11-unix --env QT_X11_NO_MITSHM=1 --net host eb2a
   ```
   

---
### **Tips for Docker**
You can open another terminal in the docker image by joining the same container, first check which containers are running:
1. **Check running containers**:
   ```bash
   docker ps
   ```
Then you can join your container on a new terminal by entering, make sure to replace container_name to your own:

2. **Open a new terminal in the container**:
   ```bash
   docker exec -ti container_name bash
   ```

You can also share folders with the docker image using additional volume commands, such as:
   ```bash
   docker run -it --rm --env DISPLAY=host.docker.internal:0 --volume /tmp/.X11-unix:/tmp/.X11-unix --env QT_X11_NO_MITSHM=1 --volume C:\eb2a-failsafe-uavs\Jason:/root/Agents --net host eb2a
   ```
---
