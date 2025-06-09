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
import jason.asSyntax.ASSyntax;
import jason.bb.BeliefBase;

public class DemoEmbeddedAgentArch extends DefaultEmbeddedAgArch {

    private Map<String, Integer> lastCPvals = new HashMap<>(); // stores last values for cp0 to cp31
    private Map<String, String> lastCPvals_bb = new HashMap<>();
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
                if (functor.matches("cp\\d+")) { // match cp0 to cp31
                    int cpIndex = Integer.parseInt(functor.substring(2));
                    if (cpIndex >= 0 && cpIndex < 32) {
                        // Get string value from the first term (remove quotes if present)
                        String value = l.getTerm(0).toString().replaceAll("^\"|\"$", "");

                        String lastVal = lastCPvals_bb.getOrDefault(functor, "__no_previous__");
                        if (!value.equals(lastVal)) {
                            // Remove the old belief
                            String oldBeliefStr = functor + "(\"" + lastVal + "\")[device(roscore1),source(percept)]";
                            Literal oldBelief = Literal.parseLiteral(oldBeliefStr);
                            getTS().getAg().getBB().remove(oldBelief);

                            // Add the new belief
                            String newBeliefStr = functor + "(\"" + value + "\")[device(roscore1),source(percept)]";
                            Literal newBelief = Literal.parseLiteral(newBeliefStr);
                            getTS().getAg().getBB().add(newBelief);

                            // Add the associated trigger to CPM
                            Literal percept = new LiteralImpl("cb" + cpIndex);
                            //percept.addTerm(ASSyntax.createString(value));
                            //Literal percept = new LiteralImpl("cb" + cpIndex + "(\"" + value + "\")"); //Adds a trigger for the severities
                            //System.out.println(percept);
                            Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
                            C.CPM.put(te.getPredicateIndicator(), true);

                            // Mark percept index as updated
                            percepts[cpIndex] = Boolean.TRUE;

                            // Update the last seen value
                            lastCPvals_bb.put(functor, value);
                        }
                    }
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
                            int cpIndex = Integer.parseInt(functor.substring(2));
                            if (cpIndex >= 0 && cpIndex < 32) {
                                // Get string value from the first term (remove quotes if present)
                                String value = l.getTerm(0).toString().replaceAll("^\"|\"$", "");

                                String lastVal = lastCPvals_bb.getOrDefault(functor, "__no_previous__");
                                if (!value.equals(lastVal)) {
                                    // Remove the old belief
                                    String oldBeliefStr = functor + "(\"" + lastVal + "\")[device(roscore1),source(percept)]";
                                    Literal oldBelief = Literal.parseLiteral(oldBeliefStr);
                                    getTS().getAg().getBB().remove(oldBelief);

                                    // Add the new belief
                                    String newBeliefStr = functor + "(\"" + value + "\")[device(roscore1),source(percept)]";
                                    Literal newBelief = Literal.parseLiteral(newBeliefStr);
                                    getTS().getAg().getBB().add(newBelief);

                                    // Add the associated trigger to CPM
                                    Literal percept = new LiteralImpl("cb" + cpIndex);
                                    //percept.addTerm(ASSyntax.createString(value));
                                    //Literal percept = new LiteralImpl("cb" + cpIndex + "(\"" + value + "\")"); //Adds a trigger for the severities
                                    //System.out.println(percept);
                                    Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
                                    C.CPM.put(te.getPredicateIndicator(), true);

                                    // Mark percept index as updated
                                    percepts[cpIndex] = Boolean.TRUE;

                                    // Update the last seen value
                                    lastCPvals_bb.put(functor, value);
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
