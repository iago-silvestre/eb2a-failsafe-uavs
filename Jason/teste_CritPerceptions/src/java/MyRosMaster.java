import embedded.mas.bridges.ros.IRosInterface;
import embedded.mas.bridges.ros.RosMaster;
import embedded.mas.bridges.ros.DefaultRos4EmbeddedMas;

import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import jason.asSemantics.Unifier;

import embedded.mas.bridges.ros.ServiceParameters;
import embedded.mas.bridges.ros.ServiceParam;
import java.util.Arrays;

public class MyRosMaster extends RosMaster{

    public MyRosMaster(Atom id, IRosInterface microcontroller) {
        super(id, microcontroller);
    }
    

    @Override
	public boolean execEmbeddedAction(String actionName, Object[] args, Unifier un) {		
		//execute the actions configured in the yaml file
		System.out.println("actionName: " +actionName);
		System.out.println("args: " +Arrays.toString(args));
		System.out.println("un: " +un);
        super.execEmbeddedAction(actionName, args, un);  // <- do not delete this line

		//Execute a customized actions 
          
		// The action "update_value" is realized through the writing in 2 topics */
		if(actionName.equals("teste2")){	

			Atom myAtom = new Atom("land");
			Object[] newArgs = new Object[]{};  // or 10.1d, depending on what's expected
			super.execEmbeddedAction(myAtom, newArgs, un);  // recursive call
			/*
			Atom myAtom = new Atom("goto_altitude");
			Object[] newArgs = new Object[]{10.1f};  // or 10.1d, depending on what's expected
			super.execEmbeddedAction(myAtom, newArgs, un);  // recursive call	
			*/		
		   //((DefaultRos4EmbeddedMas) this.getMicrocontroller()).rosWrite("/teste","std_msgs/String",(String)args[0]);
		}
		
		if(actionName.equals("goto")){ 
			ServiceParameters p = new ServiceParameters(); //p is the set of parameters of the requested service		  
		    p.addParameter("goal", new Float[]{Float.parseFloat(args[0].toString()), Float.parseFloat(args[1].toString()), Float.parseFloat(args[2].toString()), Float.parseFloat(args[3].toString())} ); //adding a new parameter which is an array of double		   
			System.out.println("uav" +args[0]+" going to  "+p);   
			serviceRequest("/uav1/control_manager/goto", p); 
			return true;

		}
		
		if(actionName.equals("adf")){	//adicionar belief failure -+status("failure");	   
	      ((DefaultRos4EmbeddedMas) microcontroller).rosWrite("/agent_detected_failure_uav1","std_msgs/String",(String)args[0]);

	      //adicionar belief
      	      //Literal lit = Literal.parseLiteral("value2"); 
	      //ts.getAg().getBB().add(lit);
		   return true;
	   }
	   if(actionName.equals("goto_alt")){ //handling the action "move_turtle"
			super.execEmbeddedAction("goto_altitude", args, un);  
			System.out.println("args: " +args);
			/*ServiceParameters p = new ServiceParameters(); 
			//p.addParameter("goal", Float.parseFloat(args[1].toString()));
			//p.addParameter("goal", new Float[]{Float.parseFloat(args[1].toString())});
			p.addParameter("goal", args[1]);
			//p.setValues(new Float[]{Float.parseFloat(args[1].toString())});
			System.out.println("arg1: " +args[1]);
			System.out.println("arg1: " +Float.parseFloat(args[1].toString()));
			System.out.println("arg1: " +Arrays.toString( new Float[]{Float.parseFloat(args[1].toString())}));
			System.out.println("uav" +args[0]+" going to altitude "+Float.parseFloat(args[1].toString()));   
            //System.out.println(new Float[]{Float.parseFloat(args[1].toString())});
			serviceRequest("/uav"+args[0]+"/control_manager/goto_altitude ", p); //send the service request		
			*/
            //serviceRequest("/uav1/control_manager/goto_altitude ", new Float[]{Float.parseFloat(args[1].toString())}); //send the service request	
			return true;

		}
		if(actionName.equals("land")){ //handling the action "move_turtle"
			System.out.println("uav" +args[0]+" landing"); 
			ServiceParameters p = new ServiceParameters(); 
			serviceRequest("/uav"+args[0]+"/uav_manager/land", p); //send the service request		   
			return true;

		}

		

		return true;
	}

}
