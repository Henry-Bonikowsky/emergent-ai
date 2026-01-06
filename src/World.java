import java.util.Random;

/**
 * The world: a 10x10 grid with mathematical coherence decay.
 * No designed hazards. Just space, position, and physics.
 * Landscape regenerates each episode - agent can't memorize.
 */
public class World {
    public static final int SIZE = 10;
    private static final double BASE_DECAY = 0.02;  // Base rate

    private int agentX;
    private int agentY;
    private double coherence;

    // Random parameters for this episode's landscape
    private double freqX, freqY, phaseX, phaseY;
    private Random random = new Random();
    
    // Presence decay: spots get worse the longer agent stays
    private double[][] presenceDecay = new double[SIZE][SIZE];
    private static final double PRESENCE_RATE = 0.5;  // How fast presence accumulates

    public World() {
        agentX = SIZE / 2;
        agentY = SIZE / 2;
        coherence = 1.0;
        generateLandscape();
    }

    private void generateLandscape() {
        freqX = 0.3 + random.nextDouble() * 0.8;
        freqY = 0.3 + random.nextDouble() * 0.8;
        phaseX = random.nextDouble() * Math.PI * 2;
        phaseY = random.nextDouble() * Math.PI * 2;
    }

    /**
     * Reset agent to center with full coherence.
     * Generates a new random landscape each episode.
     */
    public void reset() {
        coherence = 1.0;
        generateLandscape();
        presenceDecay = new double[SIZE][SIZE];
        spawnOnGreen();
    }

    /**
     * Reset with specific seed - ensures same landscape for all agents.
     */
    public void resetWithSeed(long seed) {
        random = new Random(seed);
        coherence = 1.0;
        generateLandscape();
        presenceDecay = new double[SIZE][SIZE];
        spawnOnGreen();
    }
    
    private void spawnOnGreen() {
        // Find the safest (lowest decay) spot
        double bestDecay = Double.MAX_VALUE;
        int bestX = SIZE / 2, bestY = SIZE / 2;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double decay = getDecayMultiplier(x, y);
                if (decay < bestDecay) {
                    bestDecay = decay;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        agentX = bestX;
        agentY = bestY;
    }

    /**
     * Get current state as input for Network 1.
     * Limited vision: 3x3 grid around agent + position + coherence = 12 values.
     */
    public double[] getState() {
        double[] state = new double[12];  // 9 cells + x + y + coherence

        // 3x3 vision around agent
        int idx = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = agentX + dx;
                int ny = agentY + dy;
                if (nx >= 0 && nx < SIZE && ny >= 0 && ny < SIZE) {
                    double decay = getDecayMultiplier(nx, ny) + presenceDecay[ny][nx];
                    state[idx++] = (decay - 10.0) / 10.0;  // normalize for new range
                } else {
                    state[idx++] = 1.0;  // walls appear as max decay
                }
            }
        }

        // Position and coherence
        state[idx++] = (agentX - SIZE / 2.0) / (SIZE / 2.0);
        state[idx++] = (agentY - SIZE / 2.0) / (SIZE / 2.0);
        state[idx++] = coherence * 2 - 1;

        return state;
    }

    /**
     * Execute an action and apply decay.
     * Action: 0=up, 1=down, 2=left, 3=right, 4=stay
     * Returns the coherence delta (always negative or zero).
     */
    public double step(int action) {
        // Try to move (action 4 = stay in place)
        int newX = agentX;
        int newY = agentY;

        switch (action) {
            case 0 -> newY--;
            case 1 -> newY++;
            case 2 -> newX--;
            case 3 -> newX++;
            case 4 -> {}  // stay
            default -> {}
        }

        // Only move if within bounds (edges simply fail)
        if (newX >= 0 && newX < SIZE && newY >= 0 && newY < SIZE) {
            agentX = newX;
            agentY = newY;
        }

        // Agent presence makes current spot worse
        presenceDecay[agentY][agentX] += PRESENCE_RATE;
        
        // Apply decay based on position (base + presence accumulation)
        double oldCoherence = coherence;
        double decayMultiplier = getDecayMultiplier(agentX, agentY) + presenceDecay[agentY][agentX];
        coherence -= BASE_DECAY * decayMultiplier;

        if (coherence < 0) {
            coherence = 0;
        }

        return coherence - oldCoherence; // delta (negative)
    }

    /**
     * Mathematical decay landscape.
     * Uses sin/cos with random parameters - different each episode.
     * Range: [0.1, 3.0] - extreme variance creates real survival pressure.
     * Some spots are 30x safer than others.
     */
    private double getDecayMultiplier(int x, int y) {
        double raw = Math.sin(x * freqX + phaseX) + Math.cos(y * freqY + phaseY);
        // Binary: negative = safe, positive = deadly
        if (raw < 0) {
            return 0.1;  // green - very safe
        } else {
            return 10.0;  // red - deadly fast
        }
    }

    /**
     * Get decay multiplier for visualization.
     */
    public double getDecayAt(int x, int y) {
        return getDecayMultiplier(x, y) + presenceDecay[y][x];
    }

    public boolean isAlive() {
        return coherence > 0;
    }

    public int getAgentX() {
        return agentX;
    }

    public int getAgentY() {
        return agentY;
    }

    public double getCoherence() {
        return coherence;
    }
}
