import java.util.Random;

public class AsyncEventGenerator implements Runnable {

    private final DemoEmbeddedAgentArchStdB arch;
    private final Random rng = new Random();
    private volatile boolean running = true;

    private long nextEventTimeNs;

    public AsyncEventGenerator(DemoEmbeddedAgentArchStdB arch) {
        this.arch = arch;
        scheduleNextEvent();
    }

    private void scheduleNextEvent() {
        long delayNs = (1_000 + rng.nextInt(2_000)) * 1_000_000L;
        nextEventTimeNs = System.nanoTime() + delayNs;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {

            long now = System.nanoTime();

            if (now >= nextEventTimeNs) {
                arch.injectCp0Event();
                scheduleNextEvent();
            }

            // very short sleep to avoid busy spin
            Thread.onSpinWait();
            /*try {
                //Thread.sleep(1);
                Thread.onSpinWait();
            } catch (InterruptedException e) {
                break;
            }*/
        }
    }
}
