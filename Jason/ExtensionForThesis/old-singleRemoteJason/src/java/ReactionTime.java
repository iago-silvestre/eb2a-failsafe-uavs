import java.util.concurrent.atomic.AtomicLong;

public final class ReactionTime {

    private static final AtomicLong eventTimeNs = new AtomicLong(-1);
    private static final AtomicLong perceptionTimeNs = new AtomicLong(-1);

    private ReactionTime() {}

    /* EVENT */
    public static void markEvent() {
        eventTimeNs.set(System.nanoTime());
    }

    public static long getEventTime() {
        return eventTimeNs.get();
    }

    public static boolean hasEvent() {
        return eventTimeNs.get() != -1;
    }

    public static void clearEvent() {
        eventTimeNs.set(-1);
    }

    /* PERCEPTION */
    public static void markPerception() {
        perceptionTimeNs.set(System.nanoTime());
    }

    public static long getPerceptionTime() {
        return perceptionTimeNs.get();
    }
}
