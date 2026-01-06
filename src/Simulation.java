import java.util.Random;

/**
 * Attempt #9: Multiple Agents, Random Optimizers
 *
 * Philosophy:
 * - Multiple agents exist in parallel
 * - Each gets a random optimizer (vanilla, momentum, Adam)
 * - No selection, no sharing, no breeding
 * - Just parallel observation of what different physics produces
 */
public class Simulation {
    private static final int NUM_AGENTS = 12;
    private static final int LOG_INTERVAL = 100;
    private static final int VISUAL_INTERVAL = 5000;  // Show best agent every N episodes

    private Agent[] agents;
    private Logger logger;
    private Random random = new Random();
    private Visualizer visualizer;
    private Agent bestAgent;

    private long totalEpisodes = 0;

    public Simulation(boolean withVisualizer) {
        agents = new Agent[NUM_AGENTS];
        logger = new Logger();

        System.out.println("=== Attempt #9: Multiple Agents, Random Optimizers ===");
        System.out.println("Agents: " + NUM_AGENTS);
        System.out.println();
        System.out.println("Agent configurations:");

        for (int i = 0; i < NUM_AGENTS; i++) {
            agents[i] = new Agent(i);
            System.out.printf("  Agent %2d: %s (lr=%.4f)%n",
                i,
                agents[i].getOptimizerType(),
                agents[i].getLearningRate());
        }
        System.out.println();

        bestAgent = agents[0];

        if (withVisualizer) {
            visualizer = new Visualizer(bestAgent.getWorld());
        }
    }

    public void run() {
        while (true) {
            long seed = random.nextLong();

            // All agents experience the same physics
            for (Agent agent : agents) {
                int steps = agent.runEpisode(seed);
                agent.recordEpisode(steps);
            }

            totalEpisodes++;

            // Find current best
            for (Agent a : agents) {
                if (a.getRollingAverage() > bestAgent.getRollingAverage()) {
                    bestAgent = a;
                }
            }

            if (totalEpisodes % LOG_INTERVAL == 0) {
                printStatus();
            }

            // Visual showcase of best agent
            if (visualizer != null && totalEpisodes % VISUAL_INTERVAL == 0) {
                runVisualEpisode();
            }
        }
    }

    private void runVisualEpisode() {
        System.out.println(">>> Showcasing best agent: " + bestAgent.getId() +
            " (" + bestAgent.getOptimizerType() + ")");

        World world = bestAgent.getWorld();
        visualizer.setWorld(world);
        Network n1 = bestAgent.getNetwork();
        Network n2 = bestAgent.getNetwork();

        world.reset();
        int steps = 0;

        while (world.isAlive()) {
            double[] state = world.getState();
            double[] output1 = n1.forward(state);

            // Same stochastic action selection as training
            int action = sampleAction(output1);

            world.step(action);
            steps++;

            visualizer.updateStats(totalEpisodes, steps, bestAgent.getRollingAverage());
        }

        System.out.println(">>> Showcase ended: " + steps + " steps");
    }

    private void printStatus() {
        System.out.printf("=== Episode %,d ===%n", totalEpisodes);

        // Sort agents by performance
        Agent[] sorted = agents.clone();
        java.util.Arrays.sort(sorted, (a, b) ->
            Double.compare(b.getRollingAverage(), a.getRollingAverage()));

        for (Agent a : sorted) {
            String marker = (a == bestAgent) ? "*" : " ";
            System.out.printf("%s Agent %2d [%s]: avg=%.1f rolling=%.1f best=%d%n",
                marker,
                a.getId(),
                a.getOptimizerType().toString().substring(0, 3),
                a.getAverage(),
                a.getRollingAverage(),
                a.getBestEver());
        }
        System.out.println();

        logger.recordEpisode((int) bestAgent.getRollingAverage(), 0, 0);
    }

    public static void main(String[] args) {
        boolean visual = true;
        for (String arg : args) {
            if (arg.equals("--no-visual")) {
                visual = false;
            }
        }
        Simulation sim = new Simulation(visual);
        sim.run();
    }

    private int sampleAction(double[] values) {
        double max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) max = values[i];
        }
        double sum = 0;
        double[] probs = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            probs[i] = Math.exp(values[i] - max);
            sum += probs[i];
        }
        for (int i = 0; i < probs.length; i++) {
            probs[i] /= sum;
        }
        double r = Math.random();
        double cumulative = 0;
        for (int i = 0; i < probs.length; i++) {
            cumulative += probs[i];
            if (r <= cumulative) return i;
        }
        return probs.length - 1;
    }
}