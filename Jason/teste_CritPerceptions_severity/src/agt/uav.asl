//////////////// Initial beliefs
status("None").
world_area(250, 250, 0, 0).
num_of_uavs(1).
camera_range(5).
std_altitude(20.0).
std_heading(0.0).
land_point(-102.0, -111.0).
land_radius(10.0).
diff(1).
my_number(1).
status_cb0("None"). 
//teste_underscore(1)[device(sample_roscore),source(percept)].

//pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW))))
//////////////// Rules
current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & ground_truth(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
//current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & uav7_odometry_gps_local_odom(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
//current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & uav8_odometry_gps_local_odom(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
//current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & uav9_odometry_gps_local_odom(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
//current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & uav10_odometry_gps_local_odom(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
//current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & uav11_odometry_gps_local_odom(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
//current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & uav12_odometry_gps_local_odom(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
my_number_string(S) :- my_number(N)
                       & .term2string(N, S).


severity_cp0(SEV) :- temp(T)  & T < 40       //Rules for Severity Detection
                  & SEV= "None".

severity_cp0(SEV) :- temp(T)  & T >= 40 & T <= 50 //& T < 70.0       //Rules for Severity Detection
                  & SEV= "Marginal".

severity_cp0(SEV) :- temp(T)  & T > 50 & T < 70 
                  & SEV= "Severe".

severity_cp0(SEV) :- temp(T)  & T >= 70
                  & SEV= "Critical".

//+temp(T): severity_cp0(SEV) <- -+cp0(SEV).
//+temp(T): severity_cp0("Critical")<- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","teste2",[]).

//.print(" cb0 severity= ", SEV). 
//+cb0("Marginal") [cr] <- .print(" cb0 severity= Marginal critJason test"). 

//+cb0("Severe") [cr]   <- .print(" cb0 severity= Severe critJason test").

+cb0 [cr]: cp0("Marginal") <- .print(" marg"). 

+cb0 [cr]: cp0("Severe")   <- .print(" sever").

+cb0 [cr]: cp0("Critical") <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","teste2",[]). //.print(" crit").

//for Standar jason
//+cp0("Marginal") <- .print(" marg"). 

//+cp0("Severe") <- .print(" sever"). 

//+cp0("Critical") <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","teste2",[]). //.print(" crit").


//////////////// Start
!start.

+!start
    <- .wait(2000);
      //embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","drop",[0.0, 0.0, 0.0]);
      .print("Started!");
      //!calculate_trajectory;//trajectory//!calculate_area;//!calculate_waypoints(1, []);// pode ser unido com os outros
      !hover.
      //!follow_trajectory(0).


//////////////// Calculating land position
+!hover
   :  severity_cp0(SEV) & SEV=="Marginal" 
   <- //-+status("hovering");//[device(sample_roscore),source(percept)]
      .wait(1000);
      //.print("teste_underscore");
      !hover.
+!hover
   :  num_of_uavs(X)
   <- //-+status("hovering");//[device(sample_roscore),source(percept)]
      .wait(20000);
      //.print("CX ",CX," , CY:",CY," , CZ:",CZ);
      //.print("teste");
      //.print("hovering");
      !hover.
      
+failure
   <- .wait(1000);
      -failure.
   
   
//Adicionar tratamento do belief failure
+!detected_failure
   <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","adf",N).

//////////////// Handling plan failure
+!detected_failure(_).
