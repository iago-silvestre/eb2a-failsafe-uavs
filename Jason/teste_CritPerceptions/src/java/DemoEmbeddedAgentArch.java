package embedded.mas.bridges.jacamo;

import embedded.mas.bridges.jacamo.DefaultEmbeddedAgArch;
import embedded.mas.bridges.jacamo.IDevice;
import embedded.mas.exception.PerceivingException;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jason.asSyntax.Literal;
import jason.asSyntax.Trigger;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.Trigger.TEOperator;
import jason.asSyntax.Trigger.TEType;
import jason.asSemantics.Circumstance;
import jason.asSemantics.TransitionSystem;
import jason.bb.BeliefBase;

public class DemoEmbeddedAgentArch extends DefaultEmbeddedAgArch {

    private Map<String, Integer> lastCPvals = new HashMap<>(); // stores last values for cp0 to cp31

    public DemoEmbeddedAgentArch() {
        super();
    }

    @Override
    public Boolean[] perceiveCP() {
        Boolean[] percepts = new Boolean[32]; // One for each cp0 to cp31
        Circumstance C = getTS().getC();
        C.CPM.clear();

        for (int i = 0; i < 32; i++) {
            percepts[i] = Boolean.FALSE;
        }
        BeliefBase bb = getTS().getAg().getBB();
        for (Object o : bb) {
            if (o instanceof Literal l) {
            String functor = l.getFunctor();
            //System.out.println("functor: "+functor);
            if (functor.matches("cp\\d+")) { 
                System.out.println("teste ");
                }
            }
        }
        for (IDevice s : this.devices) {
            try {
                Collection<Literal> p = s.getPercepts();
                if (p != null) {
                    for (Literal l : p) {
                        
                        String functor = l.getFunctor();
                        //System.out.println("functor: "+functor);
                        if (functor.matches("cp\\d+")) { // match cp0 to cp31
                            //System.out.println("teste perceiveCP");
                            int cpIndex = Integer.parseInt(functor.substring(2));
                            if (cpIndex >= 0 && cpIndex < 32) {
                                int value = Integer.parseInt(l.getTerm(0).toString());

                                Integer lastVal = lastCPvals.getOrDefault(functor, -12345);
                                if (value != lastVal) {
                                    // Remove the old belief
                                    String oldBeliefStr = functor + "(" + lastVal + ")[device(roscore1),source(percept)]";
                                    Literal oldBelief = Literal.parseLiteral(oldBeliefStr);
                                    getTS().getAg().getBB().remove(oldBelief);

                                    // Add the new belief
                                    Literal newBelief = Literal.parseLiteral(functor + "(" + value + ")[device(roscore1),source(percept)]");
                                    getTS().getAg().getBB().add(newBelief);

                                    // Add the associated trigger to CPM
									Literal percept = new LiteralImpl("cb" + cpIndex);
									Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
                                    C.CPM.put(te.getPredicateIndicator(), true);

                                    // Mark percept index as updated
                                    percepts[cpIndex] = Boolean.TRUE;

                                    // Update the last seen value
                                    lastCPvals.put(functor, value);
                                }
                            }
                        }
                    }
                }
            } catch (PerceivingException e) {
                // Do nothing on exception
            }
        }

        return percepts;
    }


}
