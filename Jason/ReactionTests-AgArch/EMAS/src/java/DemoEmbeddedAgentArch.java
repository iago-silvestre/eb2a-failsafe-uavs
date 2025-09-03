package embedded.mas.bridges.jacamo;

import embedded.mas.bridges.jacamo.DefaultEmbeddedAgArch;
import embedded.mas.exception.PerceivingException;

import java.util.*;
import jason.asSyntax.*;
import jason.asSemantics.Circumstance;
import jason.bb.BeliefBase;

/**
 * DemoEmbeddedAgentArch with generic severity-band detection for up to 32 CPs.
 * cp0 is by default bound to "temp", but this can be reconfigured.
 */
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
    private final Map<Integer, String> cpBindings = new HashMap<>();

    /** Severity tables per cp index */
    private final Map<Integer, List<Band>> severityTables = new HashMap<>();

    /** Last seen severity label for each cp */
    private final Map<Integer, String> lastSeverities = new HashMap<>();

    public DemoEmbeddedAgentArch() {
        super();

        // Default: cp0 is temperature with its severity bands
        cpBindings.put(0, "temp");
        severityTables.put(0, Arrays.asList(
            new Band(Double.NEGATIVE_INFINITY, 40, "None"),
            new Band(40, 50, "Marginal"),
            new Band(50, 70, "Severe"),
            new Band(70, Double.POSITIVE_INFINITY, "Critical")
        ));
        lastSeverities.put(0, "None"); // initial state
    }

    @Override
    public Boolean[] perceiveCP() {
        Boolean[] percepts = new Boolean[32];
        Arrays.fill(percepts, Boolean.FALSE);

        Circumstance C = getTS().getC();
        C.CPM.clear();

        // Iterate over each device
        for (IDevice s : this.devices) {
            try {
                Collection<Literal> devicePercepts = s.getPercepts();
                if (devicePercepts == null) continue;

                for (Literal l : devicePercepts) {
                    String functor = l.getFunctor();

                    // Check if this functor is bound to a CP index
                    for (Map.Entry<Integer, String> binding : cpBindings.entrySet()) {
                        int cpIndex = binding.getKey();
                        String boundFunctor = binding.getValue();

                        if (functor.equals(boundFunctor) && l.getArity() == 1 && l.getTerm(0).isNumeric()) {
                            try {
                                double val = ((NumberTerm) l.getTerm(0)).solve();

                                // Classify severity
                                String newSev = classifySeverity(cpIndex, val);

                                // Trigger if severity changed
                                String oldSev = lastSeverities.getOrDefault(cpIndex, "__none__");
                                if (!newSev.equals(oldSev)) {
                                    lastSeverities.put(cpIndex, newSev);

                                    Literal percept = new LiteralImpl("cb" + cpIndex);
                                    Trigger te = new Trigger(Trigger.TEOperator.add, Trigger.TEType.belief, percept);
                                    C.CPM.put(te.getPredicateIndicator(), true);

                                    percepts[cpIndex] = Boolean.TRUE;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            } catch (PerceivingException e) {
                e.printStackTrace();
            }
        }

        return percepts;
    }

    /** Extracts numeric value from belief base for given functor */
    private Double extractNumericValue(BeliefBase bb, String functor) {
        for (Literal l : bb) {
            if (functor.equals(l.getFunctor()) &&
                l.getArity() == 1 &&
                l.getTerm(0).isNumeric()) {
                try {
                    return ((NumberTerm) l.getTerm(0)).solve();
                } catch (Exception ignore) {}
            }
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

    /** Allow re-binding cp index to a different functor and severity table */
    public void bindCP(int cpIndex, String functor, List<Band> bands, String initialSeverity) {
        if (cpIndex < 0 || cpIndex >= 32) throw new IllegalArgumentException("cpIndex out of range");
        cpBindings.put(cpIndex, functor);
        severityTables.put(cpIndex, bands);
        lastSeverities.put(cpIndex, initialSeverity);
    }
}
