# emergent-ai

Original AI research exploring genuine emergent behavior in neural networks through environment design.

<img src="https://img.shields.io/badge/Java-21-orange" alt="Java"/> <img src="https://img.shields.io/badge/AI-Research-purple" alt="Research"/> <img src="https://img.shields.io/badge/Status-Active-green" alt="Active"/>

## Overview

This project represents genuine AI research tackling a fundamental problem: **how to create truly emergent intelligence without explicit reward functions or human-defined objectives**.

Unlike typical machine learning projects that use predefined rewards or labels, this research explores whether intelligence can emerge purely from environmental physics—no human telling the agent what's "good" or "bad", only a world with consequences.

## The Core Question

**Can we create environments where the easiest path IS the intelligent path?**

Traditional ML says: "Here's what success looks like (reward function), now optimize."

This research asks: "What if we never define success? What if we only define physics, and let agents discover what works through pure trial and error on the world itself?"

## Key Findings

### The Breakthrough (Attempt #9)

> **"The problem is always the environment, never the agent."**

After 9 documented research attempts, the fundamental insight emerged:

**Agents will ALWAYS find the easiest path.** If that path isn't intelligent, you get:
- Passivity (doing nothing wins)
- Wall-hugging (spam movement = free survival)
- Exploit-seeking rather than learning

### Why Nature Succeeded (And We Haven't)

**Nature's environments are self-correcting:**
- Exploit works → more agents use it → resources deplete → exploit fails
- Success automatically creates conditions for failure
- Agents ARE each other's environment

**Our simulations:**
- Exploit works → works forever → nothing changes
- No competition, no resource depletion, no automatic closure

### The Real Lesson

Emergence isn't found by setting up a simulation and waiting. **Emergence is engineered through environment design.**

The work isn't making smarter agents. It's designing environments where:
- The easiest path IS the intelligent path
- Exploits self-close without intervention
- "Good enough" keeps moving forever

**The environment is the code. Get that right, and emergence follows.**

## Research Attempts

### Attempt #9: Multiple Agents, Random Optimizers

**Design:**
- 12 agents run in parallel
- Random optimizer per agent: Vanilla SGD, Momentum, Adam
- Fixed learning rate: 0.08 (discovered empirically)
- Same physics for fair comparison

**Findings:**
- **Momentum dominated** vanilla SGD and Adam
- Hypothesis: Momentum's inertia helps "roll through" local minima
- Best rolling avg: ~50.7 after 3.4k episodes
- Best single episode: 499 steps (near theoretical max)

**Environment Evolution:**
1. **Center spawn**: Agents learned passivity (doing nothing won)
2. **Red spawn (worst spot)**: Forced movement, but agents found wall-hugging exploit
3. **Insight**: Every environment has exploits unless it self-corrects

### Bug Discovered: Identical Initialization

All momentum agents showed identical performance—root cause: `new Random()` with time-based seeds gave identical initial weights when called in quick succession.

**Fixed**: Now seeds with `System.nanoTime() ^ Thread.currentThread().getId() ^ random`

## Two Paths to AI

### 1. Human-Defined Learning (Modern ML)
- Define right/wrong explicitly (rewards, labels, RLHF)
- Fast, controllable, useful
- Creates tools that serve human goals
- **Not emergence—directed optimization**

### 2. Genuine Emergence (This Research)
- No definitions, only environment physics
- Trial and error on the *world*, not the agent
- Success must automatically close exploits
- Slow, unpredictable, might fail entirely
- **But if it works—it's real**

## Technical Implementation

### Neural Network from Scratch
- Pure Java implementation (no frameworks)
- 3 optimizer variants: Vanilla SGD, Momentum, Adam
- Backpropagation with configurable layer sizes
- Rolling average performance tracking

### Simulation Environment
- Grid-based world with decay mechanics
- BFS pathfinding for optimal navigation
- Same-seed episode replay for fair comparisons
- Comprehensive logging with CSV output

### Key Components

```
src/
├── Network.java       # Neural network with 3 optimizer types
├── Agent.java         # Agent wrapper with performance tracking
├── Simulation.java    # Multi-agent simulation runner
├── World.java         # Environment with decay physics
└── graph.py          # Visualization and analysis
```

## Literature Influences

- **Tierra** (Thomas Ray) - Digital evolution
- **Evolved Virtual Creatures** (Karl Sims) - Emergent morphology
- **Autopoiesis** (Maturana & Varela) - Self-organizing systems

## Running the Simulation

```bash
cd attempts/#9/src
javac *.java
java Simulation
```

**Outputs:**
- Console logging of agent performance
- `survival_log.csv` with detailed metrics (427KB dataset)
- Rolling averages for optimizer comparison

## Why This Matters

This isn't just "train an AI to play a game." This is research into:
- How intelligence emerges without explicit goals
- What role environment plays in shaping behavior
- Whether we can design physics that guarantees intelligence

If successful, this approach could inform:
- Artificial life simulations
- Self-organizing systems
- AI that discovers novel solutions we never conceived

## Current Status

**Active Research** - The core insight (environment-first design) has been identified, but implementing truly self-correcting environments remains an open challenge.

## Stats

- **9 documented attempts** with comprehensive READMEs
- **427KB of experimental data** (survival_log.csv)
- **Comparative optimizer analysis** across thousands of episodes
- **Genuine research** into fundamental emergence problems

## License

MIT License

---

*"The agent will always find the easiest path. Make that path intelligent."*
