{ include("mission-management2.asl", mm) }

current_mission("None").
status("None").
world_area(100, 100, 0, 0).
num_of_uavs(6).
nb_participants(5).
camera_range(5).
std_altitude(6.25).
std_heading(0.0).
land_radius(10.0).
frl_charges(5).
//cnp_limit(0).
landing_x(0.0).
landing_y(0.0).
wind_speed(-20.0).
fire_pos(0.0,0.0).
critical_p(2).
oppos_fire_dir(-180).
test_list([[0.0,5.0,6.25],[3.0,5.0,6.25],[6.0,5.0,6.25],[20.0,5.0,6.25],[40.0,5.0,6.25]]).
//test_list([[0.0,5.0,6.25],[20.0,5.0,6.25],[40.0,5.0,6.25]]).
no_fire_dir_sensor.
//test_list([[5.0,5.0,6.25],[5.0,-5.0,6.25],[-5.0,-5.0,6.25],[-5.0,5.0,6.25],[5.0,5.0,6.25]]).
//timesincecommfailure(0.0).
//firetemp_list([[20.0,5.0,6.25],[40.0,5.0,6.25]]).

current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & my_number(1) & uav1_ground_truth(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & my_number(2) & uav2_ground_truth(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & my_number(3) & uav3_ground_truth(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & my_number(4) & uav4_ground_truth(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & my_number(5) & uav5_ground_truth(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).
current_position(CX, CY, CZ) :- my_frame_id(Frame_id) & my_number(6) & uav6_ground_truth(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))).

combat_traj(CT) :- wind_speed(WS) & WS >=0.0 & fire_pos(CX,CY) & std_altitude(Z)  & my_number(N)
                  & CT= [[CX-2,CY+2,Z+N],[CX+2,CY+2,Z+N],[CX+2,CY-2,Z+N],[CX-2,CY-2,Z+N]].
combat_traj(CT) :- wind_speed(WS) & WS < 0.0 & fire_pos(CX,CY) & std_altitude(Z)  & my_number(N) 
                  & CT= [[CX-2,CY-2,Z+N],[CX+2,CY-2,Z+N],[CX+2,CY+2,Z+N],[CX-2,CY+2,Z+N]].

my_ap(AP) :- my_number(N)
            & .term2string(N, S) & .concat("autopilot",S,AP).

distance(X,Y,D) :- current_position(CX, CY, CZ) & D=math.sqrt( (CX-X)**2 + (CY-Y)**2 ).
+uav1_ground_truth(header(seq(Seq),stamp(secs(Secs),nsecs(Nsecs)),frame_id(Frame_id)),child_frame_id(CFI),pose(pose(position(x(CX),y(CY),z(CZ)),orientation(x(OX),y((OY)),z((OZ)),w((OW)))),covariance(CV)),twist(twist(linear(x(LX),y(LY),z((LZ))),angular(x(AX),y((AY)),z((AZ)))),covariance(CV2))) 
   <- -+cur_pos(CX,CY).
+fire_detection(N) : N>=22000 <- !found_fire.
+battery(B) : B<=30.0 & not(low_batt) <- !low_battery.


//+temp(T)[device(roscore1),source(percept)]<- -+temp(T).
+cb0 [cr]: cp0("Severe")& not fire_dir(_) //teste Reaction time
   <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","teste2",[]).

   
+testeC1 [cr]: cp0("teste") //teste Reaction time
   <- .print("Agent testeC1");
      embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","testeC1",[]).

+testeC2 [cr]: cp0("teste") //teste Reaction time
   <- .print("Agent testeC2");
      embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","testeC2",[]).

+testeC3 [cr]: cp0("teste") //teste Reaction time
   <- .print("Agent testeC3");
      embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","testeC3",[]).




/*
severity_cp0(SEV) :- temp(T)  & T < 40       //Rules for Severity Detection
                  & SEV= "None".

severity_cp0(SEV) :- temp(T)  & T >= 40 & T <= 50   
                  & SEV= "Marginal".

severity_cp0(SEV) :- temp(T)  & T > 50 & T < 70 
                  & SEV= "Severe".

severity_cp0(SEV) :- temp(T)  & T >= 70
                  & SEV= "Critical".


+temp(T): severity_cp0(SEV)  <- -+cp0(SEV).*/
//+temp(T)[device(roscore1),source(percept)]<- -+temp(T).

+cp0("Severe")   //Standard Jason Execution
   : not lock & my_number(N) & cur_pos(CX, CY)
   <- +lock;embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","cp0-Severe",[N,CX,CY]).


//+temp(T): severity_cp0("Critical")<- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","teste2",[]).
//+oppos_fire_dir(OFD) <- -no_fire_dir_sensor.

/*+cb0 [cr]
   : cp0("Marginal") & temp(T)[device(roscore1),source(percept)] & current_position(CX, CY, CZ) 
   <- .print("Calling cp0-Marginal with CX=", CX);
      embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","cp0-Marginal",[T]).*/


//Rules for Reaction of cb0 - Harmful Event of High Temperature 
//+cb0 [cr]: cp0("Marginal") & test(T) <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","cp0-Marginal",[T]).//.print("T = ",T). ?frl_charges(FRL);.print("FRL = ",FRL);
//+cb0 [cr]: cp0("Marginal") <- ?critical_p(N);.print("N = ",N);embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","cp0-Marginal",[N]).

//+cb0 [cr]: cp0("Marginal") & distance(0,0,D) <- .print("D = ",D).

+cb0 [cr]: cp0("Marginal") 
   <- ?my_speed(S);
      ?cur_pos(CX, CY);
      embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","cp0-Marginal",[S,CX,CY]).   

/*
+cb0 [cr]: cp0("Severe")& not fire_dir(_) 
   <- ?my_number(N);
      ?cur_pos(CX, CY);
      embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","cp0-Severe",[N,CX,CY]).*/



+cb0 [cr]: cp0("Severe") & fire_dir(_) 
   <- ?my_number(N);
      ?cur_pos(CX, CY);
      ?fire_dir(FD);
      embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","cp0-Severe-fd",[N,CX,CY,FD]).  

+cb0 [cr]: cp0("Critical") 
   <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","landhome",[]). //.print(" teste no ofd").



//+cb0 [cr]: cp0("Critical")  <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","teste2",[]).  

/*+cp0("Marginal")
   :  current_mission(CM)
   <- !mm::change_state(CM,suspended);
      .print("post critJason action - cp0");
      .wait(10000);
      !mm::resume(CM).*/
      
//+cp0("Marginal")
//   <- .print("post critJason reaction - cp0").


+!setMaxSpeed(S) // Does not work, gets overriden by MRS MPC Tracker
	<- .set_fcu_param("MPC_XY_VEL_MAX", [0, S]).

+!react_cp0 : my_number(N) <- ?my_number(N);.print("N:", N).

+react_cp1 [cr] : my_number(N) <- ?my_number(N);.print("N:", N).


+!cp0_Minor <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","cp0_Minor",[]).

+cp1_Major [cr]  <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","cp1_Major",[]). 





/*+react_cp1 [cr] : true
   <- ?my_number(N);
      ?cur_pos(CX, CY);
      .print("Fire detected by Drone: ",N," in X: ",CX," , Y:",CY).*/

//!start2.

+!start2
<- !test.

+!test
<- //.print("testing!");
   .wait(100);
   !test.

+!start
   : my_ap(AP) & my_number(N) 
    <- .wait(2000);
      +mm::my_ap(AP);
      embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","sethome",[true,0.0,0.0,0.0,0.0]);
      .print("Started! Calculating Traj");
      !calculate_trajectory;
      !my_missions.

+fireSize(0)
   : current_mission(combat_fire)
   <- !mm::stop_mission(combat_fire,"Fire is Extinguished").
      
+fireSize(0)
   : current_mission(goto_fire)
   <- !mm::stop_mission(goto_fire,"Fire is Extinguished").


+!my_missions
   :  waypoints_list(LA) & my_number(N) & my_landing_position(LAX, LAY) & std_altitude(Z)  & test_list(L)
   <- !mm::create_mission(search, 10, []); 
      !mm::create_mission(fireManeuver, 10, []); 
      !mm::create_mission(test, 10, []); 
      +mm::mission_plan(search,L); 
      !mm::create_mission(rtl, 10, []); 
      +mm::mission_plan(rtl,[[0,0,Z]]);
      //-+cp0("Severe");
      .wait(1000);
      !mm::run_mission(search);
      .wait(1000).
      //!test.


      
+!my_missions
   :  waypoints_list(L) & my_number(N) & not (N==1)
   <- !mm::create_mission(search, 10, []); 
      +mm::mission_plan(search,L); 
      !mm::run_mission(search).

+frl_charges(0)
   : my_number(N)
   <- .print(" No more Fire Retardant charges, going to recharge");
      !mm::create_mission(low_frl, 10, []); 
      +mm::mission_plan(low_frl,[[0,0,N*5]]);
      !mm::run_mission(low_frl);
      .wait(1000);
      !analyze_CNP.

+!low_battery
   : my_number(N)
   <- +low_batt;
      .print(" Low Battery, going back to Recharge");
      !mm::create_mission(low_batt, 10, []); 
      +mm::mission_plan(low_batt,[[0,0,N*5]]);
      !mm::run_mission(low_batt).

+mm::mission_state(low_batt,finished) 
   : my_number(N)
   <- .print(" Recharging Battery");
      .wait(10000);
      embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","recharge_battery",N);
      .print(" Recharged!!");  
      -low_batt.
      
+mm::mission_state(low_frl,finished) 
   <- .print(" Recharging FRL");
      .wait(10000);
      .print(" Recharged!!");  
      -+frl_charges(4);
      !analyzeFire.

+!analyzeFire
   : fireSize(FS) & FS >0 & fire_pos(CX,CY) & std_altitude(Z) & my_number(N)
   <- .print(" Going back to fire!!");
      !mm::create_mission(goto_fire, 10, []); 
      +mm::mission_plan(goto_fire,[[CX,CY,Z+N]]);
      !mm::run_mission(goto_fire).

+!found_fire
   : current_position(CX, CY, CZ) & std_altitude(Z) & my_number(N)
   & current_mission(search) & fireSize(FS) & frl_charges(FRL)
   <- +fire_pos(CX,CY);
      .print("Fire detected in X: ",CX," , Y:",CY);
      .print("FRL dif: ",(FS-FRL));
      !mm::create_mission(combat_fire, 10, [drop_when_interrupted]);
      ?combat_traj(CT);
      +mm::mission_plan(combat_fire,CT);
      !mm::run_mission(combat_fire);
      !analyze_CNP.

+!analyze_CNP
   : fireSize(FS) & frl_charges(FRL) & FS > FRL & not cnp_limit
   <- .print("FS: ",FS,"' and FR: ",FRL);
      +cnp_limit;
      !cnp( 2,help,(FS-FRL)).

+mm::mission_state(combat_fire,finished)  
   : fireSize(FS) & FS==0
   <- .print("Fire Extinguished").

/*+frl_charges(FRL)     // Probably need to remove this and use analyze cnp on combat fire end
   : fireSize(FS) & current_mission(combat_fire) & FS>FRL
   <- .print("FS: ",FS,"' and FR: ",FRL);
      !cnp( 2,help,(FS-FRL)).*/


+mm::mission_state(combat_fire,finished) 
   : frl_charges(FRL) & FRL>=1
   <- embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","fightFire",FRL);
      -+frl_charges(FRL-1);
      !mm::run_mission(combat_fire);
      .wait(1000);
      !analyze_CNP.
      //!mm::run_mission(combat_fire).

price(_Service,X,Y,R) :- 
   current_position(X, Y, CZ) & 
   frl_charges(R).
@c1 +cfp(CNPId,Task)[source(A)]
   :  price(Task,X,Y,R)
   <- +proposal(CNPId,Task,X,Y,R); 
      .send(A,tell,propose(CNPId,X,Y,R)).

@r1 +accept_proposal(CNPId)[source(A)]
   :  proposal(CNPId,Task,X,Y,R) & fire_pos(CX,CY) & std_altitude(Z) & my_number(N)
   <- .print("My proposal '",R,"' was accepted for CNP ",CNPId, ", task ",Task," for agent ",A,"!");
      .print("Going to fire in : ",CX," , ",CY);  
      !mm::create_mission(goto_fire, 10, []); 
      +mm::mission_plan(goto_fire,[[CX,CY,Z+N]]);
      !mm::run_mission(goto_fire).
   
@r2 +reject_proposal(CNPId)
   <- .print("My proposal was not accepted for CNP ",CNPId, ".");
      -proposal(CNPId,_,_,_,_). 

+!cnp(Id,Task,TR)
   <- !call(Id,Task);
      !bids(Id,LO,TR);
      !result(Id,LO,TR).
+!call(Id,Task)
   : fire_pos(CX,CY)
   <- .broadcast(tell,cfp(Id,Task));
      .broadcast(tell,fire_pos(CX,CY)).
+!bids(Id,LOS,TR) 
    : nb_participants(LP)
   <- .wait(all_proposals_received(Id,LP), 3000, _);
      .findall( offer(U,R,D,A),
                propose(Id,X,Y,R)[source(A)] & distance(X,Y,D) & U=math.abs(TR-R),
                LO);
      .sort(LO,LOS);
      .print("Offers are ",LOS).

+!result(_,[],_).
+!result(CNPId,[offer(_,R,_,WAg)|T],RT) 
    : RT > 0
   <- .send(WAg,tell,accept_proposal(CNPId));
      ND = RT-R;
      .findall(
          offer(NU,R1,D1,A1),
          .member(offer(N1,R1,D1,A1),T) & NU=math.abs(ND-R1),
          LO);
      .sort(LO,LOS);
      !result(CNPId,LOS,ND).
+!result(CNPId,[offer(_,_,_,LAg)|T],RT) 
   <- .send(LAg,tell,reject_proposal(CNPId));
      !result(CNPId,T,RT).

all_proposals_received(CNPId,NP) :-              
     .count(propose(CNPId,_,_,_)[source(_)], NO) &   
     .count(refuse(CNPId)[source(_)], NR) &      
     NP = NO + NR.

+mm::mission_state(goto_fire,finished) 
   : fire_pos(CX,CY) & std_altitude(Z) & my_number(N) & combat_traj(CT)
   <- .print("Go to fire finished!");
      !mm::create_mission(combat_fire, 10, [drop_when_interrupted]);
      +mm::mission_plan(combat_fire,CT);
      !mm::run_mission(combat_fire).

+mm::mission_state(search,finished) 
   : my_number(N) & current_position(CX, CY, CZ) 
   <- .broadcast(tell, finished_trajectory(N));
      !wait_for_others.

+mm::mission_state(waiting,finished) 
   <- !wait_for_others.

+!wait_for_others
   :  my_landing_position(LAX, LAY) & std_altitude(Z)
      & .count(finished_trajectory(_), C) & nb_participants(C)
   <- .print("All finished, going to land position");
      !mm::create_mission(goto_land, 10, []); 
      +mm::mission_plan(goto_land,[[LAX,LAY,Z]]);
      !mm::run_mission(goto_land).

+!wait_for_others 
   <-.wait(1000);
      !wait_for_others.

+mm::mission_state(goto_land,finished) 
   <- .print(" Arrived at landing point, landing!");
       embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","land",[]).

+mm::mission_state(rtl,finished) 
   <- .print(" Returned to Launch, landing!");
       embedded.mas.bridges.jacamo.defaultEmbeddedInternalAction("roscore1","land",[]).

+!calculate_trajectory
   :  my_number(N)
      & landing_x (LX)
      & landing_y (LY)
      & land_radius(R)
      & num_of_uavs(NumOfUavs)
      & world_area(H, W, CX, CY)
   <- .print("Calculating landing position");
      -+status("calculating_land_position");
      LndNumOfColumns = NumOfUavs/2;
      LndRectangleHeight = R/2;
      LndRectangleWidth = R/LndNumOfColumns;
      My_landing_x = LX - R/2 + LndRectangleWidth/2 + ((N-1) mod LndNumOfColumns)*LndRectangleWidth;
      My_landing_y = LY - R/2 + LndRectangleHeight/2 + (math.floor((N-1)/LndNumOfColumns))*LndRectangleHeight;
      +my_landing_position(My_landing_x, My_landing_y);
      .print("Calculating area");
      +status("calculating_area");
      AreaNumOfColumns = NumOfUavs/2;
      AreaRectangleHeight = H/2;
      AreaRectangleWidth = W/AreaNumOfColumns;
      X1 = CX - W/2 + ((N-1) mod AreaNumOfColumns)*AreaRectangleWidth;
      X2 = CX - W/2 + ((N-1) mod AreaNumOfColumns + 1)*AreaRectangleWidth;
      Y1 = CY - H/2 + (math.floor((N-1)/AreaNumOfColumns))*AreaRectangleHeight;
      Y2 = CY - H/2 + (math.floor((N-1)/AreaNumOfColumns) + 1)*AreaRectangleHeight;
      +my_area(X1, X2, Y1, Y2);
      !calculate_waypoints(1, []).

+!calculate_waypoints(C, OldWayList)
    :   camera_range(CR)
        & my_area(X1, X2, Y1, Y2)
        & X2 - (C+2)*CR/2 >= X1
        & std_altitude(Z)
    <-  .print("Calculating waypoints");
        -+status("calculating_waypoints");
        Waypoints = [
                        [X1 + C*CR/2, Y1 + CR/2, Z]
                        , [X1 + C*CR/2, Y2 - CR/2, Z]
                        , [X1 + (C+2)*CR/2, Y2 - CR/2, Z]
                        , [X1 + (C+2)*CR/2, Y1 + CR/2, Z]
                    ];
        .concat(OldWayList, Waypoints, NewWayList);
        !calculate_waypoints(C+4, NewWayList).

+!calculate_waypoints(_, WayList)
    <-  .print("Finished calculating waypoints");
        +waypoints_list(WayList);
        +waypoints_list_len(.length(WayList));
        .print("Waypoints list: ", WayList).

+mm::mission_state(Id,S)
   <- .print("Mission ",Id," state is ",S).

+mm::mission_step(Id,Step)
   :  mm::mission_plan(Plan)
   <- L = .length(Plan);
      R = L - Step;
      -+remaining_wp(R).

+mm::current_mission(Id)
   <- -current_mission(_);
      +current_mission(Id).

+!found_fire.
+!my_missions.
+!analyzeFire.
+!analyze_CNP.
+!comm_failure.
+!recovering_from_cp0_severe.  