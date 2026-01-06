import java.util.Random;

/**
 * Single-network agent. Pure physics teaching.
 * state -> action -> consequence -> learning
 */
public class Agent {
    private int id;
    private World world;
    private Network network;

    private Network.OptimizerType optimizerType;
    private double learningRate;

    private long totalEpisodes = 0;
    private long totalSteps = 0;
    private double rollingAverage = 0;
    private int bestEver = 0;

    private static final Random random = new Random();

    public Agent(int id) {
        this.id = id;
        this.world = new World();

        Network.OptimizerType[] types = Network.OptimizerType.values();
        this.optimizerType = types[random.nextInt(types.length)];
        this.learningRate = 0.08;

        // Single network: 12 inputs -> 5 action outputs
        network = new Network(12, new int[]{16, 8}, 5, learningRate, optimizerType);
    }

    public int runEpisode(long seed) {
        world.resetWithSeed(seed);
        int steps = 0;

        while (world.isAlive()) {
            double[] state = world.getState();
            double[] output = network.forward(state);

            int action = sampleAction(output);
            double delta = world.step(action);

            // Learn directly from physics
            double actionValue = (delta + 0.03) * 30;
            double[] target = new double[5];
            target[action] = actionValue;
            network.learn(target, world.getCoherence());

            steps++;
        }

        return steps;
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

    public void recordEpisode(int steps) {
        totalEpisodes++;
        totalSteps += steps;
        if (steps > bestEver) bestEver = steps;
        if (rollingAverage == 0) {
            rollingAverage = steps;
        } else {
            rollingAverage = 0.99 * rollingAverage + 0.01 * steps;
        }
    }

    public int getId() { return id; }
    public Network.OptimizerType getOptimizerType() { return optimizerType; }
    public double getLearningRate() { return learningRate; }
    public long getTotalEpisodes() { return totalEpisodes; }
    public double getRollingAverage() { return rollingAverage; }
    public int getBestEver() { return bestEver; }
    public double getAverage() { return totalEpisodes > 0 ? (double) totalSteps / totalEpisodes : 0; }

    public World getWorld() { return world; }
    public Network getNetwork() { return network; }
}
