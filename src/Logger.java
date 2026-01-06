import java.io.*;
import java.util.LinkedList;

/**
 * Tracks survival times, rolling averages, and saves checkpoints.
 */
public class Logger {
    private static final String CHECKPOINT_DIR = "checkpoints";

    private LinkedList<Integer> recentSurvivalTimes;
    private long totalEpisodes;
    private PrintWriter logWriter;

    public Logger() {
        recentSurvivalTimes = new LinkedList<>();
        totalEpisodes = 0;

        // Create checkpoint directory
        new File(CHECKPOINT_DIR).mkdirs();

        // Open log file
        try {
            logWriter = new PrintWriter(new FileWriter("survival_log.csv"));
            logWriter.println("generation,avg_survival,n1_weight_mag,n2_weight_mag");
        } catch (IOException e) {
            System.err.println("Could not create log file: " + e.getMessage());
        }
    }

    /**
     * Record a generation's average survival.
     */
    public void recordEpisode(int avgSteps, double n1WeightMag, double n2WeightMag) {
        totalEpisodes++;

        // Log every generation to file
        if (logWriter != null) {
            logWriter.printf("%d,%d,%.4f,%.4f%n",
                totalEpisodes, avgSteps, n1WeightMag, n2WeightMag);
            logWriter.flush();
        }
    }

    public double getRollingAverage() {
        if (recentSurvivalTimes.isEmpty()) return 0;
        double sum = 0;
        for (int t : recentSurvivalTimes) {
            sum += t;
        }
        return sum / recentSurvivalTimes.size();
    }

    public long getTotalEpisodes() {
        return totalEpisodes;
    }


    /**  
     * Save network weights to checkpoint file.
     */
    public void saveCheckpoint(Network n1, Network n2) {
        String filename = String.format("%s/checkpoint_ep%d.dat", CHECKPOINT_DIR, totalEpisodes);
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(filename))) {
            double[] w1 = n1.getWeights();
            double[] w2 = n2.getWeights();

            out.writeInt(w1.length);
            for (double w : w1) out.writeDouble(w);

            out.writeInt(w2.length);
            for (double w : w2) out.writeDouble(w);

            System.out.println("Checkpoint saved: " + filename);
        } catch (IOException e) {
            System.err.println("Could not save checkpoint: " + e.getMessage());
        }
    }

    /**
     * Load network weights from checkpoint file.
     */
    public static void loadCheckpoint(String filename, Network n1, Network n2) {
        try (DataInputStream in = new DataInputStream(new FileInputStream(filename))) {
            int len1 = in.readInt();
            double[] w1 = new double[len1];
            for (int i = 0; i < len1; i++) w1[i] = in.readDouble();
            n1.setWeights(w1);

            int len2 = in.readInt();
            double[] w2 = new double[len2];
            for (int i = 0; i < len2; i++) w2[i] = in.readDouble();
            n2.setWeights(w2);

            System.out.println("Checkpoint loaded: " + filename);
        } catch (IOException e) {
            System.err.println("Could not load checkpoint: " + e.getMessage());
        }
    }

    public void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}
