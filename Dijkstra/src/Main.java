import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DijkstraSelfStabilization {
    private static final int N = 4;
    private static final int[] processes_states = {0,0,0,0};

    private static final int NUM_PROCESSES = N;
    private static final int NUM_THREADS = N;
    private static final Process[] processes = new Process[NUM_PROCESSES];
    private static volatile boolean systemStabilized = false;
    private static final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    private static final Object token = new Object();
    private static volatile int tokenHolder = -1;

    public static void main(String[] args) {
        for (int i = 0; i < NUM_PROCESSES; i++) {
            processes[i] = new Process(i, processes_states[i]);
        }

        printInitialStates();

        for (int i = 0; i < NUM_PROCESSES; i++) {
            executor.execute(processes[i]);
        }

        monitorStabilization();

        executor.shutdown();
    }

    static void printInitialStates() {
        System.out.print("Initial states: ");
        for (int i = 0; i < NUM_PROCESSES; i++) {
            System.out.print("Process " + i + ": " + processes[i].getState() + " ");
        }
        System.out.println();
    }

    static void printCurrentStates() {
        System.out.print("Current states after stabilization: ");
        for (int i = 0; i < NUM_PROCESSES; i++) {
            System.out.print("Process " + i + ": " + processes[i].getState() + " ");
        }
        System.out.println();
    }

    static void printTokenHolder() {
        System.out.println(" | Token holder: Process " + tokenHolder);
    }

    static void monitorStabilization() {
        new Thread(() -> {
            try {
                while (!systemStabilized) {
                    if (checkStabilization()) {
                        systemStabilized = true;
                        System.out.println("System has stabilized");

                        printCurrentStates();
                        executor.shutdown();
                    }

                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    static boolean checkStabilization() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    static class Process implements Runnable {
        private final int id;
        private int state;

        public Process(int id, int initialState) {
            this.id = id;
            this.state = initialState;
        }

        @Override
        public void run() {
            try {
                while (!systemStabilized) {
                    int previousState = processes[(id - 1 + N) % N].getState();

                    synchronized (token) {
                        if (id == 0 && previousState == processes[N - 1].getState()) {
                            state = (previousState + 1) % (N + 1);
                            tokenHolder = id;  // Process 0 gets the token
                            System.out.print("Process " + id + " updated state to " + state);
                            printTokenHolder();
                        } else if (id != 0 && previousState != state) {
                            state = previousState;
                            tokenHolder = id;
                            System.out.print("Process " + id + " updated state to " + state);
                            printTokenHolder();
                        }
                    }

                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public int getState() {
            return state;
        }
    }
}
