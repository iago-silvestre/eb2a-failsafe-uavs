package embedded.mas.bridges.jacamo;

import embedded.mas.bridges.jacamo.DefaultEmbeddedAgArch;
import embedded.mas.bridges.jacamo.IDevice;

import java.util.Collection;
import embedded.mas.exception.PerceivingException;
import jason.asSyntax.Literal;
import jason.asSyntax.Trigger;
import jason.asSemantics.Circumstance;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.Trigger.TEOperator;
import jason.asSyntax.Trigger.TEType;



public class DemoEmbeddedAgentArch extends DefaultEmbeddedAgArch{

	private int lastCPval = -12345; //LBB: this is temporary, requires a better implementation

    public DemoEmbeddedAgentArch() {
		super();
	}
				
    @Override
	public Boolean[] perceiveCP() { //v0: Still not generic, works only for the hand-coded perception "value2"
		Boolean[] percepts = new Boolean[8];
		Circumstance C = getTS().getC();
		C.CPM.clear();
		for(int i=0; i<8; i++)
			percepts[i] = Boolean.FALSE;
		
		for(IDevice s:this.devices) { //for each sensor
			try {
				Collection<Literal> p = s.getPercepts(); //List<Literal> beliefs = microcontroller.read(); return beliefs;
				if(p!=null) {
					for(Literal l:p) {
						String functor = l.getFunctor();
						if(functor.contains("critical_percept")){ //This needs to be adjusted to support multiple critical percepts
							int value = Integer.parseInt(l.getTerm(0).toString());
							if(value != lastCPval){
								Literal cpToRemove = Literal.parseLiteral("critical_percept(" + lastCPval + ")[device(roscore1),source(percept)]"); //Remove the old belief
								getTS().getAg().getBB().remove(cpToRemove); //this is done to avoid triggering critical reactions based on old beliefs
								lastCPval = value;
								percepts[0] = Boolean.TRUE;
								Literal cp0Percept = Literal.parseLiteral("critical_percept(" + value + ")[device(roscore1),source(percept)]"); // This needs to be adjusted to use variable name
								getTS().getAg().getBB().add(cp0Percept);       // adds the belief to be used in context of estimating the severity of the event
								Literal percept = new LiteralImpl("cb0");  // This needs to be adjusted to use variable name
                				Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
                				C.CPM.put(te.getPredicateIndicator(), true);
							}
						}
							
					}
				}
			} catch (PerceivingException e) {} //if it fails, do nothing 			
		}
		//getTS().getAg().getBB().remove(Literal.parseLiteral("critical_percept(" + lastCPval + ")"));
		//Literal oldBelief = Literal.parseLiteral("critical_percept(" + lastCPval + ")");
		//getTS().getAg().getBB().remove(oldBelief);
		return percepts;
   	}
  
	
}
