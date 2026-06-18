/**
 * CommonsDemo: shows the self-closing exploit the project's own conclusion called for.
 *
 *   "Exploit works -> more agents use it -> resources deplete -> exploit fails.
 *    Success automatically creates failure conditions. Agents ARE each other's
 *    environment."  -- README
 *
 * The single-agent World never did this: each agent owned its landscape, so an
 * exploit paid forever. On a shared Commons it does not. This prints two fixed
 * strategies for the same agents on identical commons and shows the crossover where
 * piling onto the best cell stops paying.
 *
 *   javac Commons.java CommonsDemo.java && java CommonsDemo
 */
public class CommonsDemo {

    public static void main(String[] args) {
        int grid = 4;                 // 16 cells
        int ticks = 150;
        double cap = 100.0, frac = 0.4, regen = 0.12, rivalry = 0.4;

        System.out.println("=== Commons: the self-closing exploit ===");
        System.out.println("grid=" + grid + "x" + grid + "  ticks=" + ticks
                + "  capacity=" + cap + "  extract=" + frac
                + "  regen=" + regen + "  rivalry=" + rivalry);
        System.out.println();
        System.out.printf("%-8s %14s %14s %12s%n",
                "agents", "crowd/agent", "spread/agent", "winner");
        System.out.println("-------------------------------------------------------");

        for (int m : new int[]{1, 2, 4, 8, 12, 16}) {
            double crowd = cumulativePerAgent(grid, ticks, cap, frac, regen, rivalry, m, true);
            double spread = cumulativePerAgent(grid, ticks, cap, frac, regen, rivalry, m, false);
            String winner = Math.abs(crowd - spread) < 1e-9 ? "tie"
                    : (spread > crowd ? "SPREAD" : "crowd");
            System.out.printf("%-8d %14.2f %14.2f %12s%n", m, crowd, spread, winner);
        }

        System.out.println();
        System.out.println("At 1 agent the strategies are identical. As the crowd grows the");
        System.out.println("shared cell congests and depletes, so spreading out wins: the");
        System.out.println("exploit closed itself, with no reward function telling it to.");

        // Show the resource trace of the hammered cell under a full crowd.
        System.out.println();
        System.out.println("Best-cell resource under a 16-agent crowd (every 30 ticks):");
        Commons crowd = new Commons(grid, cap, frac, regen, rivalry);
        for (int t = 1; t <= ticks; t++) {
            crowd.beginTick();
            for (int a = 0; a < grid * grid; a++) crowd.register(0, 0);
            crowd.resolveHarvest();
            crowd.regenerate();
            if (t % 30 == 0) {
                System.out.printf("  tick %3d: resource=%.1f   per-agent yield=%.3f%n",
                        t, crowd.resourceAt(0, 0), peekYield(crowd, grid, cap, frac, regen, rivalry));
            }
        }
    }

    private static double cumulativePerAgent(int grid, int ticks, double cap, double frac,
                                             double regen, double rivalry, int m, boolean crowd) {
        Commons c = new Commons(grid, cap, frac, regen, rivalry);
        double total = 0.0;
        for (int t = 0; t < ticks; t++) {
            c.beginTick();
            if (crowd) {
                for (int a = 0; a < m; a++) c.register(0, 0);
                total += c.yieldAt(0, 0);
            } else {
                for (int a = 0; a < m; a++) c.register(a % grid, (a / grid) % grid);
                double tick = 0.0;
                for (int a = 0; a < m; a++) tick += c.yieldAt(a % grid, (a / grid) % grid);
                total += tick / m;
            }
            c.resolveHarvest();
            c.regenerate();
        }
        return total;
    }

    // Per-agent yield a full crowd would get on the best cell at its current resource.
    private static double peekYield(Commons c, int grid, double cap, double frac,
                                    double regen, double rivalry) {
        return Commons.perAgentYield(c.resourceAt(0, 0), grid * grid, frac, rivalry);
    }
}
