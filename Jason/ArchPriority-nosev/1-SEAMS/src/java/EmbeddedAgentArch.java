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
import jason.asSemantics.Event;
import jason.asSemantics.Intention;
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
    private final Map<Integer, String> cpBindings = new LinkedHashMap<>();

    /** Last seen severity label for each cp */
    private final Map<Integer, String> lastVals = new HashMap<>();

    // RosMaster added for instant trigger of Critical Severity perceptions
    private MyRosMaster myRosMaster;

    private static final Map<String, Integer> cpToPriority = new HashMap<>();
    static {
        Map.of(
            5, List.of("cp2"),             //Catastrophic
            4, List.of("cpTeste" ),                   //Hazardous
            3, List.of("cp1"),                  //Major
            2, List.of("cp0"),            //Minor
            1, List.of("cpTeste")             //No Effect
        ).forEach((prio, cps) -> cps.forEach(cp -> cpToPriority.put(cp, prio)));
    }

    private static final Map<Integer, String> priorityToMode = new HashMap<>();
    static {
        priorityToMode.put(5, "Bypass");
        priorityToMode.put(4, "Expedited-RC");
        priorityToMode.put(3, "Expedited-RC");
        priorityToMode.put(2, "Standard-RC");
        priorityToMode.put(1, "Standard-RC");
    }

    /** Mapping of cp functor -> reaction string (e.g., "cp1" -> "react_cp1") */
    private static final Map<String, String> cpReactions = new HashMap<>();
    static {
        cpReactions.put("cp0", "cp0_Minor");    // !react_cp0
        cpReactions.put("cp1", "cp1_Major");    // react_cp1 [cr]
        cpReactions.put("cp2", "cp2_Catastrophic");  //  internalAction(cp2-Catastrophic)
        cpReactions.put("cp3", "react_cp3");    
        cpReactions.put("cp4", "failsafe_2");
        cpReactions.put("cp5", "react_cp5");
        cpReactions.put("cp6", "react_cp6");
        cpReactions.put("cp7", "failsafe_2");
        cpReactions.put("cp8", "react_cp8");
        cpReactions.put("cp9", "cp9_catastrophic");
    }

    public DemoEmbeddedAgentArch() {
        super();

        // Bind cp0 directly to "cp0"
        cpToPriority.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // Descending priority
        .forEach(entry -> {
            String cp = entry.getKey(); // e.g., "cp7"
            try {
                int cpIndex = Integer.parseInt(cp.replace("cp", ""));
                cpBindings.put(cpIndex, cp);
                lastVals.put(cpIndex, "None");
            } catch (NumberFormatException e) {
                System.err.println("Invalid CP key format: " + cp);
            }
        });

        myRosMaster = new MyRosMaster(new Atom("roscore1"),new DefaultRos4EmbeddedMas("ws://0.0.0.0:9090", new ArrayList<>(), new ArrayList<>()) );

    
    }

    /** Helper: assign priority based on cp functor and severity */
    private int getPriority(String functor) {
        return cpToPriority.getOrDefault(functor, 3);
    }

    /** Small container for CP entries to be inserted in CPM later */
    private static class CPEntry {
        int cpIndex;
        String functor;
        int priority;

        CPEntry(int cpIndex, String functor, int priority) {
            this.cpIndex = cpIndex;
            this.functor = functor;
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

        // Loop over all cpBindings
        for (Map.Entry<Integer, String> binding : cpBindings.entrySet()) {
            String functor = binding.getValue(); // e.g., "cp0"

            if (!hasBelief(bb, functor)) continue;

            int priority = getPriority(functor);
            String reaction = cpReactions.getOrDefault(functor, "handle_" + functor);
            String mode = priorityToMode.get(priority);

            try {
                switch (mode) {
                    case "Bypass":
                        myRosMaster.execEmbeddedAction(reaction, new Object[]{}, null);
                        break;

                    case "Expedited-RC":
                        Literal percept = new LiteralImpl(reaction);
                        Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
                        C.CPM.put(te.getPredicateIndicator(), true);
                        break;

                    default: // Standard-RC;
                        Trigger goal = new Trigger(TEOperator.add, TEType.achieve, ASSyntax.createLiteral(reaction));
                        getTS().getC().addEvent(new Event(goal, null));
                        
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Remove the belief after execution
            bb.remove(ASSyntax.createLiteral(functor));
        }
        return percepts;
    }

    private String getCurrentTime() {
        return java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSSSS"));
    }

    private boolean hasBelief(BeliefBase bb, String functor) {
        try {
            Literal pattern = ASSyntax.createLiteral(functor);
            Iterator<Literal> it = bb.getCandidateBeliefs(pattern, null);
            return it != null && it.hasNext();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Extracts value string directly from a cpX("S") belief */
    private String extractVal(BeliefBase bb, String functor) {
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
