# emergent-ai

Research project exploring whether intelligence can emerge from environment design alone, without explicit reward functions.

**Status: Completed** | **Java 21**

## The Question

Can you create environments where the easiest path IS the intelligent path? What if you never tell the agent what's "good" or "bad", only define physics and let them figure it out?

Most ML uses predefined rewards ("here's what success looks like, now optimize"). This research asked: what if we only define physics and let agents discover what works through pure trial and error?

## What I Found

After 9 documented attempts, the core insight:

**"The problem is always the environment, never the agent."**

Agents will ALWAYS find the easiest path. If that path isn't intelligent, you get:
- Passivity (doing nothing wins)
- Wall-hugging (spam movement for free survival)
- Exploit-seeking instead of learning

### Why Nature Works

Nature's environments self-correct:
- Exploit works → more agents use it → resources deplete → exploit fails
- Success automatically creates failure conditions
- Agents ARE each other's environment

Our simulations:
- Exploit works → works forever → nothing changes
- No competition, no depletion, no automatic closure

### The Real Lesson

Emergence isn't found by setting up a simulation and waiting. **Emergence is engineered through environment design.**

The work isn't making smarter agents. It's designing environments where:
- The easiest path IS the intelligent path
- Exploits self-close without intervention
- "Good enough" keeps moving forever

## Research Attempts

### Attempt #9: Multiple Agents, Random Optimizers

**Setup:**
- 12 agents in parallel
- Random optimizer per agent: Vanilla SGD, Momentum, Adam
- Fixed learning rate: 0.08
- Same physics for fair comparison

**Results:**
- Momentum dominated vanilla SGD and Adam
- Best rolling avg: ~50.7 after 3.4k episodes
- Best single episode: 499 steps (near theoretical max)
- Found bug: identical initialization for same-optimizer agents

**Environment Evolution:**
1. Center spawn: agents learned passivity
2. Red spawn: forced movement, found wall-hugging exploit
3. Insight: every environment has exploits unless it self-corrects

## Technical Implementation

### Neural Network from Scratch
- Pure Java (no frameworks)
- 3 optimizer variants: Vanilla SGD, Momentum, Adam
- Backpropagation with configurable layers
- Rolling average performance tracking

### Simulation
- Grid world with decay mechanics
- BFS pathfinding
- Same-seed episode replay for fair comparisons
- CSV logging (427KB dataset)

```
src/
├── Network.java       # Neural network with 3 optimizers
├── Agent.java         # Agent wrapper with stats
├── Simulation.java    # Multi-agent runner
├── World.java         # Environment with decay
└── graph.py          # Visualization
```

## Why This Matters

This isn't "train an AI to play a game." It's research into:
- How intelligence emerges without explicit goals
- What role environment plays in shaping behavior
- Whether we can design physics that guarantees intelligence

## Stats

- 9 documented attempts with READMEs
- 427KB experimental data
- Comparative optimizer analysis across thousands of episodes

## Running

```bash
cd attempts/#9/src
javac *.java
java Simulation
```

## License

MIT License

---

*"The agent will always find the easiest path. Make that path intelligent."*
