//package embedded.mas.bridges.jacamo;

import embedded.mas.bridges.jacamo.DefaultEmbeddedAgArch;
import embedded.mas.bridges.jacamo.IDevice;
import embedded.mas.exception.PerceivingException;

import java.util.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//import embedded.mas.bridges.ros.MyRosMaster;
import embedded.mas.bridges.ros.DefaultRos4EmbeddedMas;
import embedded.mas.bridges.ros.IRosInterface;
import embedded.mas.bridges.jacamo.LiteralDevice;
import jason.asSemantics.ConcurrentInternalAction;

import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;
import jason.asSyntax.*;
import jason.asSyntax.Trigger;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.Trigger.TEOperator;
import jason.asSyntax.Trigger.TEType;
import jason.asSemantics.Circumstance;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.ASSyntax;
import jason.bb.BeliefBase;

public class DemoEmbeddedAgentArch extends DefaultEmbeddedAgArch {

    /** Severity band definition */
    private static class Band {
        final double min, max;
        final String label;
        Band(double min, double max, String label) {
            this.min = min; this.max = max; this.label = label;
        }
        boolean matches(double v) { return v > min && v <= max; }
    }

    /** Mapping of cpX → functor name (e.g., cp0 = "temp") */
    private final Map<Integer, String> cpBindings = new ConcurrentHashMap<>();

    /** Severity tables per cp index */
    private final Map<Integer, List<Band>> severityTables = new ConcurrentHashMap<>();

    /** Last seen severity label for each cp */
    private final Map<Integer, String> lastSeverities = new ConcurrentHashMap<>();

    // RosMaster added for instant trigger of Critical Severity perceptions
    private MyRosMaster myRosMaster;

 
    public DemoEmbeddedAgentArch() {
        super();

        // Default: cp0 is temperature with its severity bands
        cpBindings.put(0, "temp");
        severityTables.put(0, Arrays.asList(
            new Band(Double.NEGATIVE_INFINITY, 40, "None"),
            //new Band(40, 50, "Marginal"),
            new Band(40, 50, "teste"),
            //new Band(50, 70, "Severe"),
            new Band(50, 70, "teste2"),
            new Band(70, Double.POSITIVE_INFINITY, "Critical")
        ));
        lastSeverities.put(0, "None"); // initial state
        
        //myRosMaster = new MyRosMaster(new Atom("roscore2"),new DefaultRos4EmbeddedMas("ws://0.0.0.0:9090", new ArrayList<>(), new ArrayList<>()));
        myRosMaster = new MyRosMaster(new Atom("roscore1"),new DefaultRos4EmbeddedMas("ws://0.0.0.0:9090",new ArrayList<>() ,null));
    }


    @Override
    public Boolean[] perceiveCP() {

        Boolean[] percepts = new Boolean[32]; // One for each cp0 to cp31
        Circumstance C = getTS().getC();
        C.CPM.clear();
        BeliefBase bb = getTS().getAg().getBB();

        for (IDevice dev : this.devices) {   //Instantly adding beliefs from devices to BB
            try {
                
                Collection<Literal> perceptsDevice = dev.getPercepts();
                
                if (perceptsDevice == null) continue;
                //System.out.println("reading : "+perceptsDevice);
                for (Literal l : perceptsDevice) {
                    // optionally add annotations here if you want
                    //System.out.println("updating : "+ l);
                    bb.add(l);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        for (int i = 0; i < 32; i++) {
            percepts[i] = Boolean.FALSE;
        }

        

        // For each configured CP with severity table
        for (Map.Entry<Integer, String> binding : cpBindings.entrySet()) {
            int cpIndex = binding.getKey();
            
            String functor = binding.getValue();

            // Look for functor(term) in the belief base
            //System.out.println(functor+"[device(roscore1),source(percept)]");
            Double val = extractNumericValue(bb, functor);
            //System.out.println("functor :"+functor + " |val: "+val);
            if (val == null) continue;
            //System.out.println("val: "+val);
            // Classify severity
            String newSev = classifySeverity(cpIndex, val);

            // If severity changed, trigger cbX
            String oldSev = lastSeverities.getOrDefault(cpIndex, "__none__");
            if (!newSev.equals(oldSev)) {
                System.out.println("newSec: "+newSev);
                if ("Critical".equals(newSev)) {
                    try {
                        myRosMaster.execEmbeddedAction("teste2", new Object[]{}, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                lastSeverities.put(cpIndex, newSev);
                // Remove the old belief
                String oldBeliefStr = "cb"+cpIndex + "(\"" + oldSev + "\")[source(self)]";
                //System.out.println("oldBelief: "+oldBeliefStr);
                Literal oldBelief = Literal.parseLiteral(oldBeliefStr);
                getTS().getAg().getBB().remove(oldBelief);

                String newBeliefStr = "cb"+cpIndex + "(\"" + newSev + "\")[source(self)]";
                //System.out.println("newBelief: "+newBeliefStr);
                Literal newBelief = Literal.parseLiteral(newBeliefStr);
                getTS().getAg().getBB().add(newBelief);

                
                if ("Severe".equals(newSev)) {
                    // Trigger cbX
                    Literal percept = new LiteralImpl("cb" + cpIndex);
                    Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
                    C.CPM.put(te.getPredicateIndicator(), true);
                    percepts[cpIndex] = Boolean.TRUE;
                    System.out.println("Executing through EB2A");
                }


                if ("teste2".equals(newSev)) { 
                    //System.out.println("val: "+val);
                    Literal percept = new LiteralImpl("testeC1");
                    Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
                    C.CPM.put(te.getPredicateIndicator(), true);

                    System.out.println(C.CPM);
                }   
                if ("teste".equals(newSev)) {
                    //System.out.println("val: "+val);
                    Literal percept3 = new LiteralImpl("testeC3");
                    Trigger te3 = new Trigger(TEOperator.add, TEType.belief, percept3);
                    C.CPM.put(te3.getPredicateIndicator(), true);
                    //System.out.println(te3.getPredicateIndicator());
                    
                    Literal percept = new LiteralImpl("testeC1");
                    Trigger te = new Trigger(TEOperator.add, TEType.belief, percept);
                    C.CPM.put(te.getPredicateIndicator(), true);
                    System.out.println(te.getPredicateIndicator());    

                    percepts[0] = Boolean.TRUE;
                    percepts[1] = Boolean.TRUE;
                    

                    Literal percept2 = new LiteralImpl("testeC2");
                    Trigger te2 = new Trigger(TEOperator.add, TEType.belief, percept2);
                    C.CPM.put(te2.getPredicateIndicator(), true);
                    System.out.println(te2.getPredicateIndicator());

                    System.out.println(C.CPM);
                }
            }
        }
        
        return percepts;
    }

    /** Extracts numeric value from belief base for given functor */
    private Double extractNumericValue(BeliefBase bb, String functor) {
        try {
            // Create a pattern like functor(X)
            Literal pattern = ASSyntax.createLiteral(functor, ASSyntax.createVar("X"));  //temp(X)
            Unifier u = new Unifier();

            Iterator<Literal> it = bb.getCandidateBeliefs(pattern, u);

            if (it == null) {
                return null; // no candidates at all
            }

            while (it.hasNext()) {
                Literal l = it.next();
                Term t = l.getTerm(0);
                if (t.isNumeric()) {
                    return ((NumberTerm) t).solve();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Classify value into severity band for given cp index */
    private String classifySeverity(int cpIndex, double val) {
        List<Band> bands = severityTables.get(cpIndex);
        if (bands == null) return "Unknown";
        for (Band b : bands) {
            if (b.matches(val)) return b.label;
        }
        return "Unknown";
    }


}
