//package embedded.mas.bridges.jacamo;

import embedded.mas.bridges.jacamo.DefaultEmbeddedAgArch;
import embedded.mas.bridges.jacamo.IDevice;
import embedded.mas.exception.PerceivingException;

import java.util.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

//import embedded.mas.bridges.ros.MyRosMaster;
import embedded.mas.bridges.ros.DefaultRos4EmbeddedMas;
import embedded.mas.bridges.ros.IRosInterface;
import embedded.mas.bridges.jacamo.LiteralDevice;
import jason.asSemantics.ConcurrentInternalAction;

import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.*;
import jason.asSyntax.Trigger;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.Trigger.TEOperator;
import jason.asSyntax.Trigger.TEType;
import jason.asSemantics.Circumstance;
import jason.asSemantics.TransitionSystem;
import jason.bb.BeliefBase;

public class DemoEmbeddedAgentArch extends DefaultEmbeddedAgArch {

    /** Mapping of cpX → functor name */
    private final Map<Integer, String> cpBindings = new HashMap<>();

    /** Last seen severity label for each cp */
    private final Map<Integer, String> lastSeverities = new HashMap<>();

    // RosMaster added for instant trigger of Critical Severity perceptions
    private MyRosMaster myRosMaster;

    public DemoEmbeddedAgentArch() {
        super();

        // Bind cp0 directly to "cp0"
        cpBindings.put(0, "cp0");
        lastSeverities.put(0, "None"); // initial state
        cpBindings.put(1, "cp1");
        lastSeverities.put(1, "None"); // initial state
        cpBindings.put(2, "cp2");
        lastSeverities.put(2, "None"); // initial state
        cpBindings.put(3, "cp3");
        lastSeverities.put(3, "None"); // initial state

        myRosMaster = new MyRosMaster(new Atom("roscore1"),new DefaultRos4EmbeddedMas("ws://0.0.0.0:9090", new ArrayList<>(), new ArrayList<>()) );
    }

    /** Helper: assign priority based on cp functor and severity */
    private int getPriority(String functor, String severity) {
        if ("cp0".equals(functor)) { // Example mapping for temperature (cp0)
            switch (severity) {
                case "Marginal": return 2;   
                case "Severe":   return 3;
                case "Critical": return 4;
            }
        }

        /*if ("cp3".equals(functor)) { // Example mapping for motorFailure (cp3)
            switch (severity) {
                case "Marginal": return 3;
                case "Severe":   return 4;  // Hazardous
                case "Critical": return 5;  //Catastrophic
            }
        }*/

        // Default mapping for other cpX (can be adjusted later)
        switch (severity) {
            case "Marginal": return 1;  // No effect
            case "Severe":   return 2;  // Minor
            case "Critical": return 3;  // Major
            default: return 0; 
        }
    }

    /** Small container for CP entries to be inserted in CPM later */
    private static class CPEntry {
        int cpIndex;
        String functor;
        String severity;
        int priority;

        CPEntry(int cpIndex, String functor, String severity, int priority) {
            this.cpIndex = cpIndex;
            this.functor = functor;
            this.severity = severity;
            this.priority = priority;
        }
    }

    @Override
    public Boolean[] perceiveCP() {

        Boolean[] percepts = new Boolean[32]; // One for each cp0 to cp31
        Circumstance C = getTS().getC();
        C.CPM.clear();

        // Step 1: Add beliefs from devices to BB
        for (IDevice dev : this.devices) {
            try {
                Collection<Literal> perceptsDevice = dev.getPercepts();
                if (perceptsDevice == null) continue;

                for (Literal l : perceptsDevice) {
                    getTS().getAg().getBB().add(l);
                    //System.out.println("l added:" +l);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Arrays.fill(percepts, Boolean.FALSE);
        BeliefBase bb = getTS().getAg().getBB();

        // Temporary list to collect cp entries (with priorities) and be inserted in CPM after loop
        List<CPEntry> tempList = new ArrayList<>();

        // Step 2: For each configured CP belief, check severity directly
        for (Map.Entry<Integer, String> binding : cpBindings.entrySet()) {
            int cpIndex = binding.getKey();
            String functor = binding.getValue(); // e.g., "cp0"
            //System.out.println("checking for func: "+functor);

            String newSev = extractSeverity(bb, functor);
            if (newSev == null) continue;
            //System.out.println("newSev: "+newSev);
            String oldSev = lastSeverities.getOrDefault(cpIndex, "__none__");
            if (!newSev.equals(oldSev)) {

                if ("Critical".equals(newSev) && cpIndex == 0) {
                    try {
                        myRosMaster.execEmbeddedAction("teste2", new Object[]{}, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                lastSeverities.put(cpIndex, newSev);

                // Remove old cbX belief
                String oldBeliefStr = "cb" + cpIndex + "(\"" + oldSev + "\")[source(self)]";
                Literal oldBelief = Literal.parseLiteral(oldBeliefStr);
                getTS().getAg().getBB().remove(oldBelief);

                // Add new cbX belief
                String newBeliefStr ="cb" + cpIndex + "(\"" + newSev + "\")[source(self)]";
                Literal newBelief = Literal.parseLiteral(newBeliefStr);
                getTS().getAg().getBB().add(newBelief);

                // Determine priority and collect for CPM insertion later
                int priority = getPriority(functor, newSev);
                tempList.add(new CPEntry(cpIndex, functor, newSev, priority));
                if ("teste".equals(newSev)) {
                    System.out.println("Priority Test | Multiple EOI in same cycle");
                    String newBeliefStrcp1 ="cp1(\"Marginal\")[source(self)]";
                    Literal newBeliefcp1 = Literal.parseLiteral(newBeliefStrcp1);
                    getTS().getAg().getBB().add(newBeliefcp1);
                    String newBeliefStrcp2 ="cp2(\"Critical\")[source(self)]";
                    Literal newBeliefcp2 = Literal.parseLiteral(newBeliefStrcp2);
                    getTS().getAg().getBB().add(newBeliefcp2);
                    String newBeliefStrcp3 ="cp3(\"Severe\")[source(self)]";
                    Literal newBeliefcp3 = Literal.parseLiteral(newBeliefStrcp3);
                    getTS().getAg().getBB().add(newBeliefcp3);
                }


            }
        }

        // Step 3: Sort collected CPs by priority (highest first) and update CPM
        tempList.sort((a, b) -> Integer.compare(b.priority, a.priority));
        for (CPEntry entry : tempList) {
            // The original code added a Trigger for cbX into C.CPM
            Literal percept = new LiteralImpl("cb" + entry.cpIndex);
            Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
            C.CPM.put(te.getPredicateIndicator(), true);
            System.out.println("C.CPM : "+C.CPM);

        }

        return percepts;
    }

    /** Extracts severity string directly from a cpX("Severity") belief */
    private String extractSeverity(BeliefBase bb, String functor) {
        try {
            Literal pattern = ASSyntax.createLiteral(functor, ASSyntax.createVar("S"));
            Unifier u = new Unifier();

            Iterator<Literal> it = bb.getCandidateBeliefs(pattern, u);
            if (it == null) return null;

            while (it.hasNext()) {
                Literal l = it.next();
                Term t = l.getTerm(0);
                if (t.isString()) {
                    return ((StringTerm) t).getString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
