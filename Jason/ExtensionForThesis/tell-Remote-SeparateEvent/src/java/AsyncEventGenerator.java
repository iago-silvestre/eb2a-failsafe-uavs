import java.util.Random;

public class AsyncEventGenerator implements Runnable {

    private final DemoEmbeddedAgentArchStdB arch;
    private final Random rng = new Random();
    private volatile boolean running = true;

    public AsyncEventGenerator(DemoEmbeddedAgentArchStdB arch) {
        this.arch = arch;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // sleep 1–3 seconds
                int sleepMs = 1000 + rng.nextInt(2000);
                Thread.sleep(sleepMs);

                // signal event
                arch.injectCp0Event();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
