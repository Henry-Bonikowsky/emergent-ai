import java.util.Random;

/**
 * Commons: a shared, depletable, self-regenerating resource field.
 *
 * This is the piece the project's own conclusion named but never built. In the
 * single-agent World each agent has its OWN landscape, so an exploit that works
 * works forever: nothing else touches it, nothing depletes, nothing pushes back.
 * Nature self-corrects because agents ARE each other's environment: an exploit
 * draws a crowd, the crowd depletes the resource and congests the spot, and the
 * exploit stops paying. Success creates the conditions of its own failure.
 *
 * Commons makes that real with two coupled mechanics, both deterministic:
 *
 *   1. Rivalry (congestion). A cell offers a fixed fraction of its resource per
 *      tick, split among everyone harvesting it, and crowding wastes some of the
 *      take. Per-agent yield therefore STRICTLY falls as occupancy rises, so the
 *      best cell stops being best once everyone piles onto it.
 *
 *   2. Depletion + logistic regrowth. What is harvested leaves the cell; the cell
 *      regrows logistically toward capacity. Harvest slower than regrowth and the
 *      cell is sustainable; harvest faster and it collapses, and a collapsed cell
 *      (resource 0) stays dead. Over-exploitation is self-terminating.
 *
 * The static helpers are the pure, verifiable core (see CommonsTest). The instance
 * wraps them over a grid and a per-tick occupancy map.
 */
public class Commons {

    /**
     * Per-agent yield for one cell in one tick.
     *
     * The cell offers extractFraction * cellResource total. That offer is split
     * evenly among the occupants, then each agent's share is degraded by congestion
     * (rivalry) that grows with the number of rivals. The result is strictly
     * decreasing in occupants for any cellResource > 0: the mathematical statement
     * of "crowding an exploit makes it worse for everyone".
     *
     * @param cellResource    current resource in the cell, >= 0
     * @param occupants       agents harvesting this cell this tick, >= 0
     * @param extractFraction fraction of resource the cell offers per tick, (0, 1]
     * @param rivalry         congestion coefficient, >= 0 (0 = pure even split)
     * @return per-agent yield, or 0 if nobody is harvesting
     */
    public static double perAgentYield(double cellResource, int occupants,
                                       double extractFraction, double rivalry) {
        if (occupants <= 0 || cellResource <= 0.0) {
            return 0.0;
        }
        double offered = extractFraction * cellResource;
        double congestion = 1.0 + rivalry * (occupants - 1);
        return offered / (occupants * congestion);
    }

    /**
     * Total resource actually removed from a cell in one tick. Equal to the offer
     * scaled down by congestion: offered / congestion. This never exceeds the offer
     * (and so never exceeds the resource, since extractFraction <= 1), which is what
     * keeps the field conserved and non-negative.
     */
    public static double totalHarvested(double cellResource, int occupants,
                                        double extractFraction, double rivalry) {
        return perAgentYield(cellResource, occupants, extractFraction, rivalry) * occupants;
    }

    /**
     * One logistic regrowth step toward capacity, clamped to [0, capacity].
     *
     * next = r + regen * r * (1 - r / capacity)
     *
     * A cell strictly below capacity (and above 0) grows; capacity is a fixed point;
     * a fully depleted cell (r = 0) stays dead. That last fact is deliberate: it is
     * the teeth behind "over-harvest self-terminates".
     */
    public static double logisticRegen(double r, double capacity, double regen) {
        double next = r + regen * r * (1.0 - r / capacity);
        if (next < 0.0) return 0.0;
        if (next > capacity) return capacity;
        return next;
    }

    // --- instance: the shared grid ------------------------------------------

    public final int size;
    private final double capacity;
    private final double extractFraction;
    private final double regen;
    private final double rivalry;

    private final double[][] resource;
    private final int[][] occupants;

    /** Full-capacity commons. */
    public Commons(int size, double capacity, double extractFraction, double regen, double rivalry) {
        this.size = size;
        this.capacity = capacity;
        this.extractFraction = extractFraction;
        this.regen = regen;
        this.rivalry = rivalry;
        this.resource = new double[size][size];
        this.occupants = new int[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                resource[y][x] = capacity;
            }
        }
    }

    /** Commons seeded with a random resource landscape in [0, capacity]. */
    public Commons(int size, double capacity, double extractFraction, double regen,
                   double rivalry, long seed) {
        this(size, capacity, extractFraction, regen, rivalry);
        Random rng = new Random(seed);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                resource[y][x] = rng.nextDouble() * capacity;
            }
        }
    }

    /** Clear occupancy at the start of a tick. */
    public void beginTick() {
        for (int y = 0; y < size; y++) {
            java.util.Arrays.fill(occupants[y], 0);
        }
    }

    /** Register one agent harvesting the given cell this tick. */
    public void register(int x, int y) {
        occupants[y][x]++;
    }

    /** Per-agent yield an agent would realize at a cell given current occupancy. */
    public double yieldAt(int x, int y) {
        return perAgentYield(resource[y][x], occupants[y][x], extractFraction, rivalry);
    }

    /**
     * Resolve all registered harvests for this tick: remove the harvested resource
     * from every occupied cell. Returns the grid-wide total harvested. Resource is
     * guaranteed to stay in [0, capacity].
     */
    public double resolveHarvest() {
        double total = 0.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int occ = occupants[y][x];
                if (occ <= 0) continue;
                double taken = totalHarvested(resource[y][x], occ, extractFraction, rivalry);
                resource[y][x] -= taken;
                if (resource[y][x] < 0.0) resource[y][x] = 0.0;  // float guard
                total += taken;
            }
        }
        return total;
    }

    /** Logistic regrowth of every cell toward capacity. */
    public void regenerate() {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                resource[y][x] = logisticRegen(resource[y][x], capacity, regen);
            }
        }
    }

    public double resourceAt(int x, int y) {
        return resource[y][x];
    }

    public int occupantsAt(int x, int y) {
        return occupants[y][x];
    }

    public double totalResource() {
        double sum = 0.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                sum += resource[y][x];
            }
        }
        return sum;
    }

    public double getCapacity() {
        return capacity;
    }
}
