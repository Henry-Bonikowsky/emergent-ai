import java.util.Random;

/**
 * Self-verifying tests for Commons. Plain Java, no framework: prints each check and
 * exits nonzero on the first failure, so `java CommonsTest` is a pass/fail gate.
 *
 * The per-cell yield and regrowth formulas were derived and differentially tested
 * through the Hermes harness; these tests pin the formula edge cases AND the
 * system-level properties the whole feature exists to demonstrate: conservation,
 * over-harvest collapse, sustainable equilibrium, and the self-closing crossover.
 */
public class CommonsTest {

    private static int checks = 0;

    public static void main(String[] args) {
        yieldStrictlyDecreasingInOccupants();
        yieldZeroEdges();
        harvestNeverExceedsResource();
        regenFixedPointsAndGrowth();
        regenClampsAndStaysInBounds();
        resourceStaysInBoundsOverManyTicks();
        overHarvestCollapses();
        lightHarvestIsSustainable();
        exploitSelfCloses();

        System.out.println();
        System.out.println("ALL PASS (" + checks + " checks)");
    }

    // 1. Per-agent yield strictly falls as more agents pile onto a cell. This is the
    //    mathematical core of "crowding an exploit makes it worse for everyone".
    private static void yieldStrictlyDecreasingInOccupants() {
        double res = 100.0, frac = 0.5, rivalry = 0.3;
        double prev = Double.MAX_VALUE;
        for (int occ = 1; occ <= 20; occ++) {
            double y = Commons.perAgentYield(res, occ, frac, rivalry);
            check(y < prev, "yield must strictly decrease at occupants=" + occ
                    + " (" + y + " !< " + prev + ")");
            prev = y;
        }
        // Even with zero rivalry it still falls, because the cell's offer is shared.
        check(Commons.perAgentYield(res, 2, frac, 0.0)
                < Commons.perAgentYield(res, 1, frac, 0.0),
                "rivalrous split must fall with occupants even at rivalry=0");
    }

    // 2. Nobody harvesting, or nothing to harvest, yields exactly 0.
    private static void yieldZeroEdges() {
        check(Commons.perAgentYield(100.0, 0, 0.5, 0.3) == 0.0, "occupants=0 -> 0");
        check(Commons.perAgentYield(100.0, -3, 0.5, 0.3) == 0.0, "occupants<0 -> 0");
        check(Commons.perAgentYield(0.0, 5, 0.5, 0.3) == 0.0, "resource=0 -> 0");
        check(Commons.perAgentYield(-5.0, 5, 0.5, 0.3) == 0.0, "resource<0 -> 0");
    }

    // 3. Conservation: the total pulled from a cell in a tick can never exceed what
    //    the cell holds. Fuzzed over many deterministic random configurations.
    private static void harvestNeverExceedsResource() {
        Random rng = new Random(20260618L);
        for (int i = 0; i < 5000; i++) {
            double res = rng.nextDouble() * 1000.0;
            int occ = rng.nextInt(50);
            double frac = rng.nextDouble();            // (0,1]
            double rivalry = rng.nextDouble() * 5.0;
            double taken = Commons.totalHarvested(res, occ, frac, rivalry);
            check(taken <= res + 1e-9, "harvest " + taken + " exceeded resource " + res);
            check(taken >= 0.0, "harvest went negative: " + taken);
        }
    }

    // 4. Regrowth fixed points and growth direction.
    private static void regenFixedPointsAndGrowth() {
        double cap = 100.0, regen = 0.1;
        check(Commons.logisticRegen(cap, cap, regen) == cap, "capacity is a fixed point");
        check(Commons.logisticRegen(0.0, cap, regen) == 0.0, "dead cell stays dead");
        check(Commons.logisticRegen(0.0, cap, regen) == 0.0
                && Commons.logisticRegen(0.0, cap, 5.0) == 0.0, "regen cannot revive r=0");
        double mid = 40.0;
        check(Commons.logisticRegen(mid, cap, regen) > mid, "0<r<cap with regen>0 must grow");
        check(Commons.logisticRegen(mid, cap, 0.0) == mid, "regen=0 holds steady");
    }

    // 5. Regrowth never leaves [0, capacity], including aggressive regen that would
    //    overshoot without the clamp.
    private static void regenClampsAndStaysInBounds() {
        double cap = 50.0;
        // A large regen rate near mid-capacity overshoots the cap pre-clamp.
        double hot = Commons.logisticRegen(45.0, cap, 5.0);
        check(hot <= cap && hot >= 0.0, "overshoot must clamp to capacity, got " + hot);
        Random rng = new Random(7L);
        for (int i = 0; i < 5000; i++) {
            double r = rng.nextDouble() * cap;
            double regen = rng.nextDouble() * 6.0;
            double next = Commons.logisticRegen(r, cap, regen);
            check(next >= 0.0 && next <= cap, "regen left bounds: " + next);
        }
    }

    // 6. Full instance loop: register/resolve/regenerate over many random ticks keeps
    //    every cell in [0, capacity].
    private static void resourceStaysInBoundsOverManyTicks() {
        Commons c = new Commons(5, 100.0, 0.4, 0.15, 0.5, 42L);
        Random rng = new Random(99L);
        for (int tick = 0; tick < 2000; tick++) {
            c.beginTick();
            int agents = rng.nextInt(30);
            for (int a = 0; a < agents; a++) {
                c.register(rng.nextInt(5), rng.nextInt(5));
            }
            c.resolveHarvest();
            c.regenerate();
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 5; x++) {
                    double r = c.resourceAt(x, y);
                    check(r >= 0.0 && r <= 100.0, "cell out of bounds: " + r);
                }
            }
        }
    }

    // 7. Over-harvest is self-terminating: harvest one cell harder than it can regrow
    //    and it collapses to ~0 and stays there.
    private static void overHarvestCollapses() {
        Commons c = new Commons(1, 100.0, 0.6, 0.05, 0.2);
        for (int tick = 0; tick < 200; tick++) {
            c.beginTick();
            for (int a = 0; a < 10; a++) c.register(0, 0);  // a crowd hammering the cell
            c.resolveHarvest();
            c.regenerate();
        }
        check(c.resourceAt(0, 0) < 1.0,
                "over-harvested cell should collapse, got " + c.resourceAt(0, 0));
    }

    // 8. Light harvest is sustainable: a single agent taking less than the cell
    //    regrows leaves it healthy indefinitely.
    private static void lightHarvestIsSustainable() {
        Commons c = new Commons(1, 100.0, 0.05, 0.2, 0.0);
        for (int tick = 0; tick < 500; tick++) {
            c.beginTick();
            c.register(0, 0);                              // one agent, light touch
            c.resolveHarvest();
            c.regenerate();
        }
        check(c.resourceAt(0, 0) > 50.0,
                "lightly harvested cell should stay healthy, got " + c.resourceAt(0, 0));
    }

    // 9. THE HEADLINE: the exploit self-closes. Compare two fixed strategies for the
    //    same M agents over the same ticks on identical commons:
    //      CROWD  - all M agents hammer the single best cell every tick.
    //      SPREAD - the M agents fan out, one per cell.
    //    At M=1 they are identical. As M grows, SPREAD's per-agent take must overtake
    //    CROWD's. That overtaking IS "success creates the conditions of its own failure".
    //
    //    The model self-closes by TWO mechanisms depending on rivalry: at low rivalry
    //    the crowded cell is drained to collapse (see overHarvestCollapses); at the
    //    higher rivalry used here congestion wastes most of the take, so the cell
    //    survives but pays each agent almost nothing (futility). We assert futility
    //    here: the crowded per-agent yield is a small fraction of a solo agent's.
    private static void exploitSelfCloses() {
        int grid = 4;                 // 16 cells, enough to fan out
        int ticks = 150;
        double cap = 100.0, frac = 0.4, regen = 0.12, rivalry = 0.4;

        double gapAtOne = perAgentGap(grid, ticks, cap, frac, regen, rivalry, 1);
        check(Math.abs(gapAtOne) < 1e-9,
                "at M=1 crowd and spread must be identical, gap=" + gapAtOne);

        int full = grid * grid;       // every agent on its own cell when spread
        double gapAtFull = perAgentGap(grid, ticks, cap, frac, regen, rivalry, full);
        check(gapAtFull > 0.0,
                "at full crowd, spread must beat crowd per-agent, gap=" + gapAtFull);

        // And the gap is monotonic-ish: it is positive and growing from a mid count
        // to the full count (the exploit gets relatively worse the more pile on).
        double gapAtMid = perAgentGap(grid, ticks, cap, frac, regen, rivalry, full / 2);
        check(gapAtFull > gapAtMid,
                "self-closing pressure must intensify with crowd size ("
                        + gapAtFull + " !> " + gapAtMid + ")");

        // The mechanism here (high rivalry): crowding the exploit makes its per-agent
        // yield collapse versus harvesting it solo, so it stops being worth piling onto.
        Commons fresh = new Commons(grid, cap, frac, regen, rivalry);
        fresh.beginTick();
        fresh.register(0, 0);
        double solo = fresh.yieldAt(0, 0);

        Commons crowd = new Commons(grid, cap, frac, regen, rivalry);
        crowd.beginTick();
        for (int a = 0; a < full; a++) crowd.register(0, 0);
        double crowded = crowd.yieldAt(0, 0);

        check(crowded < solo * 0.2,
                "crowded per-agent yield should collapse vs solo (" + crowded + " !< "
                        + (solo * 0.2) + ")");
    }

    /**
     * Cumulative per-agent harvest under SPREAD minus under CROWD for M agents over
     * `ticks` ticks on identical fresh commons. Positive means spreading out paid
     * better per agent (the exploit has self-closed).
     */
    private static double perAgentGap(int grid, int ticks, double cap, double frac,
                                      double regen, double rivalry, int m) {
        Commons crowd = new Commons(grid, cap, frac, regen, rivalry);
        Commons spread = new Commons(grid, cap, frac, regen, rivalry);
        double crowdPer = 0.0, spreadPer = 0.0;

        for (int t = 0; t < ticks; t++) {
            // CROWD: all M on cell (0,0).
            crowd.beginTick();
            for (int a = 0; a < m; a++) crowd.register(0, 0);
            crowdPer += crowd.yieldAt(0, 0);          // identical for each crowded agent
            crowd.resolveHarvest();
            crowd.regenerate();

            // SPREAD: M agents fanned out one-per-cell (wrapping if M > cells).
            spread.beginTick();
            for (int a = 0; a < m; a++) spread.register(a % grid, (a / grid) % grid);
            double tickSpread = 0.0;
            for (int a = 0; a < m; a++) tickSpread += spread.yieldAt(a % grid, (a / grid) % grid);
            spreadPer += tickSpread / m;              // average per spread agent
            spread.resolveHarvest();
            spread.regenerate();
        }
        return spreadPer - crowdPer;
    }

    private static void check(boolean cond, String msg) {
        checks++;
        if (!cond) {
            System.out.println("FAIL: " + msg);
            System.exit(1);
        }
    }
}
