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
    private final Map<Integer, String> lastVals = new HashMap<>();

    // RosMaster added for instant trigger of Critical Severity perceptions
    private MyRosMaster myRosMaster;

    private static final Map<String, Integer> cpToPriority = new HashMap<>();
    static {
        Map.of(
            5, List.of("cp9"),
            4, List.of("cp8", "cp7", "cp0"),
            3, List.of("cp2", "cp4"),
            2, List.of("cp3", "cp5"),
            1, List.of("cp6", "cp1")
        ).forEach((prio, cps) -> cps.forEach(cp -> cpToPriority.put(cp, prio)));
    }

    public DemoEmbeddedAgentArch() {
        super();

        // Bind cp0 directly to "cp0"
        cpBindings.put(0, "cp0");
        lastVals.put(0, "None"); // initial state
        cpBindings.put(1, "cp1");
        lastVals.put(1, "None"); // initial state
        cpBindings.put(2, "cp2");
        lastVals.put(2, "None"); // initial state
        cpBindings.put(3, "cp3");
        lastVals.put(3, "None"); // initial state

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
                // If you kept the old signature, you can call getPriority(functor, newVal) — the second arg is ignored
                int priority = getPriority(functor);


                if (priority == 5) {
                // Catastrophic → BYPASS
                try {
                    if (cpIndex == 0) {
                        // same bypass mechanism you already use; previously gated by cpIndex==0 and Critical severity
                        myRosMaster.execEmbeddedAction("teste2", new Object[]{}, null);
                    }
                } catch (Exception e) {
                e.printStackTrace();
                }
                } else if (priority == 3 || priority == 4) {
                    // Major/Hazardous → collect to be inserted into CPM (will be sorted later)
                    tempList.add(new CPEntry(cpIndex, functor, priority));
                    System.out.println("added in tempList cpIndex: " + cpIndex + " with prio: " + priority);
                } else { // priority 1 or 2
                    // No effect/Minor → add an intention to the agent
                    // This posts an internal event to achieve a goal named handle_<cp>
                    // Adjust the goal name to whatever plan head you will handle in .asl (e.g., +!handle_cp0)
                    Trigger goal = new Trigger(TEOperator.add, TEType.achieve, ASSyntax.createLiteral("handle_" + functor));
                    getTS().getC().addEvent(new Event(goal, null));
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
        }

        if (C.CPM.size() != 0) {
            System.out.println("C.CPM : "+C.CPM);
        }

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
