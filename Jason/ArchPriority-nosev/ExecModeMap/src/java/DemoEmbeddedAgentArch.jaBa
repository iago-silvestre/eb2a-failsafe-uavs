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
        //myRosMaster = new MyRosMaster(new Atom("roscore1"),new DefaultRos4EmbeddedMas("ws://0.0.0.0:9090", new ArrayList<>(), new ArrayList<>()) );
        
    
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

        // Temporary list to collect cp entries (with priorities) and be inserted in CPM after loop
        List<CPEntry> tempList = new ArrayList<>();

        // Step 2: For each configured CP belief, check severity directly
        for (Map.Entry<Integer, String> binding : cpBindings.entrySet()) {
            int cpIndex = binding.getKey();
            String functor = binding.getValue(); // e.g., "cp0"
            //System.out.println("checking for func: "+functor);

            String newVal = extractVal(bb, functor);
            if (newVal == null) continue;
            //System.out.println("newVal: "+newVal);
            String oldVal = lastVals.getOrDefault(cpIndex, "__none__");
            
            if (!newVal.equals(oldVal)) {
                // keep current dedup behavior based on severity change
                lastVals.put(cpIndex, newVal);


                // severity is no longer considered for routing; we use priority only
                int priority = getPriority(functor);
                // Lookup reaction string for this functor. Fallback to a default goal name if not configured.
                String reaction = cpReactions.getOrDefault(functor, "handle_" + functor);
                String mode = priorityToMode.get(priority);
                // Depending on Mode the reaction will be triggered in different manners
                // Bypass  -> Direct bypass and call .defaultEmbeddedInternalAction("roscore1",reaction,[]);
                // EB2A    -> adds to CPM -> expedited deliberate of EB2A will select plans and run them
                // Std     -> adds a standard !intention on the Agent, reaction is solved through Standard RC
                if ("Bypass".equals(mode)) {
                    // Catastrophic → BYPASS using the configured reaction string
                    try {
                        //System.out.println("Bypass : "+reaction);
                        // Execute a bypass embedded action named as the reaction.
                        myRosMaster.execEmbeddedAction(reaction, new Object[]{}, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if ("Expedited-RC".equals(mode)) {
                    //System.out.println("Expedited-RC : "+reaction);
                    // Major/Hazardous → collect to be inserted into CPM (will be sorted later)
                    Literal percept = new LiteralImpl(reaction);
                    Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
                    C.CPM.put(te.getPredicateIndicator(), true);
                    //tempList.add(new CPEntry(cpIndex, reaction, priority)); For now lets rollback to direct CPM insert
                } else { // priority 1 or 2
                    //System.out.println("Standard-RC : "+reaction);
                    // No effect/Minor → add an intention to the agent
                    // This posts an internal event to achieve a goal named as the reaction string
                    // Adjust the goal name to whatever plan head you will handle in .asl (e.g., +!react_cp1)
                    Trigger goal = new Trigger(TEOperator.add, TEType.achieve, ASSyntax.createLiteral(reaction));
                    getTS().getC().addEvent(new Event(goal, null));
                }
            }

        }

        // Step 3: Sort collected CPs by priority (highest first) and update CPM
        /*tempList.sort((a, b) -> Integer.compare(b.priority, a.priority));
        for (CPEntry entry : tempList) {
            // The original code added a Trigger for cbX into C.CPM
            //Literal percept = new LiteralImpl("cb" + entry.cpIndex);
            Literal percept = new LiteralImpl(entry.functor);
            Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
            C.CPM.put(te.getPredicateIndicator(), true);
        }*/

        

        return percepts;
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
