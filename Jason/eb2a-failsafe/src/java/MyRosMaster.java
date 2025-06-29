import embedded.mas.bridges.ros.RosMaster;


import embedded.mas.bridges.ros.DefaultRos4EmbeddedMas;
import embedded.mas.bridges.ros.ServiceParameters;
import embedded.mas.bridges.ros.IRosInterface;

import jason.asSyntax.Atom;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.NumberTerm;
import jason.asSemantics.Unifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


//import jade.util.leap.ArrayList;


public class MyRosMaster extends RosMaster {

	public MyRosMaster(Atom id, IRosInterface microcontroller) {        
		super(id, microcontroller);
        System.out.println("**** Iniciando MyRosMaster ****");
	}

    private boolean exec_test_mrs_topic_action_light(Object[] args){
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode path = mapper.createObjectNode();            
        ObjectNode header = mapper.createObjectNode();
        ObjectNode stamp = mapper.createObjectNode();
        ArrayNode points = mapper.createArrayNode();
                        
        int seq = 0;
        int secs = 0;
        int nsecs = 0;
        String frame_id = "";
        int input_id = 0;          
        boolean use_heading = false; 
        boolean fly_now = true; 
        boolean stop_at_waypoints = true; 
        boolean loop = false; 
        float max_execution_time = 0.0f;
        float  max_deviation_from_path = 0.1f;
        boolean  dont_prepend_current_state = false;
        boolean  override_constraints = false;
        float  override_max_velocity_horizontal = 0.0f;
        float  override_max_acceleration_horizontal = 0.0f;
        float  override_max_jerk_horizontal = 0.0f;
        float  override_max_velocity_vertical = 0.0f;
        float  override_max_acceleration_vertical = 0.0f;
        float  override_max_jerk_vertical = 0.0f;
        boolean  relax_heading = false;

        Object[] array_points= ((ListTermImpl) args[1] ).toArray();

        for(int i=0;i<array_points.length;i++){
            Object[] array_position = ((ListTermImpl) array_points[i] ).toArray();
            //Object[] array_position = (Object[]) array_points[i];   
            float x = Float.parseFloat(array_position[0].toString());
            float y = Float.parseFloat(array_position[1].toString());
            float z = Float.parseFloat(array_position[2].toString());

            ObjectNode pointJson = mapper.createObjectNode();
            ObjectNode position = mapper.createObjectNode();   

            position.put("x", x);
            position.put("y", y);
            position.put("z", z);

            int heading = 0;

            pointJson.put("position", position);
            pointJson.put("heading", heading);

            points.add(pointJson);

        }
            
        path.put("header",header);
        path.put("input_id",input_id);
        path.put("use_heading", use_heading);
        path.put("fly_now", fly_now);
        path.put("stop_at_waypoints", stop_at_waypoints);
       //path.put("loop", loop ?"1":"0");
        path.put("loop", loop);
        path.put("max_execution_time", max_execution_time);
        path.put("max_deviation_from_path", max_deviation_from_path);
        path.put("dont_prepend_current_state", dont_prepend_current_state);
        path.put("override_constraints", override_constraints);
        path.put("override_max_velocity_horizontal", override_max_velocity_horizontal);
        path.put("override_max_acceleration_horizontal", override_max_acceleration_horizontal);
        path.put("override_max_jerk_horizontal", override_max_jerk_horizontal);
        path.put("override_max_velocity_horizontal", override_max_velocity_vertical);
        path.put("override_max_acceleration_horizontal", override_max_acceleration_vertical);
        path.put("override_max_jerk_horizontal", override_max_jerk_vertical);            
        path.put("relax_heading", relax_heading);
        path.put("points", points);
                        
        header.put("seq", seq);
        header.put("stamp",stamp);
        header.put("frame_id",frame_id);
        stamp.put("secs", secs);
        stamp.put("nsecs", nsecs);
            

        try {
            ((DefaultRos4EmbeddedMas) microcontroller).rosWrite("/uav"+args[0]+"/trajectory_generation/path","mrs_msgs/Path",mapper.writeValueAsString(path));                   
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;

    }

    
    @Override
	public boolean execEmbeddedAction(String actionName, Object[] args, Unifier un) {		
        

        if (actionName.equals("cp0-Marginal")) {
            //System.out.println("Logging event of cp0 - Marginal ");
            System.out.println("myrosmaster.java |  cp0-Marginal = " + args[0].toString());
            return true;
            }

          
        if(actionName.equals("cp0-Severe")){	

			Atom hover = new Atom("hover");
			Object[] noargs = new Object[]{};  // or 10.1d, depending on what's expected
			super.execEmbeddedAction(hover, noargs, un);  // recursive call
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace(); // or handle it another way
            }
			
			Atom gotoalt = new Atom("goto_altitude");
			Object[] altitude = new Object[]{15.0f};  // or 10.1d, depending on what's expected
			super.execEmbeddedAction(gotoalt, altitude, un);  // recursive call	
		    return true;
		   //((DefaultRos4EmbeddedMas) this.getMicrocontroller()).rosWrite("/teste","std_msgs/String",(String)args[0]);
		}
        
        if(actionName.equals("teste")){	

			Atom myAtom = new Atom("land");
			Object[] newArgs = new Object[]{};  // or 10.1d, depending on what's expected
			super.execEmbeddedAction(myAtom, newArgs, un);  // recursive call
            return true;
			/*
			Atom myAtom = new Atom("goto_altitude");
			Object[] newArgs = new Object[]{10.1f};  // or 10.1d, depending on what's expected
			super.execEmbeddedAction(myAtom, newArgs, un);  // recursive call	
			*/		
		   //((DefaultRos4EmbeddedMas) this.getMicrocontroller()).rosWrite("/teste","std_msgs/String",(String)args[0]);
		}

		
		if (actionName.equals("teste2")) {
			((DefaultRos4EmbeddedMas) microcontroller).rosWrite("/agent_detected_failure_uav1","std_msgs/String","1");
            return true;
		}

        if(actionName.equals("path")){
            return exec_test_mrs_topic_action_light(args);           
        }
        /*if(actionName.equals("goto_alt")){ //handling the action "move_turtle"

			ServiceParameters p = new ServiceParameters(); 
			p.addParameter("goal",new Float[]{Float.parseFloat(args[1].toString())});
            //System.out.println(new Float[]{Float.parseFloat(args[1].toString())});
			serviceRequest("/uav"+args[0]+"/control_manager/goto_altitude ", p); //send the service request		   
            //serviceRequest("/uav1/control_manager/goto_altitude ", new Float[]{Float.parseFloat(args[1].toString())}); //send the service request	
			return true;

		}*/
        else 
		return false;
	}
	
}

