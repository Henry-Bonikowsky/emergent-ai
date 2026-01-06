# Attempt #9: Multiple Agents, Random Optimizers

## Date
December 20, 2025

## Hypothesis
The gradient descent algorithm itself is a design choice we've been controlling. By randomizing the optimizer and learning rate for each agent, we make this "environmental" - part of the physics rather than part of our design.

## Design

### What's New
1. **Multiple agents** (12) run in parallel
2. **Random optimizer** per agent: vanilla SGD, momentum, or Adam
3. **Fixed learning rate**: 0.08 (discovered empirically - see findings)
4. **Same physics** - all agents experience identical landscapes each episode

### What Remains Human-Designed
- Network architecture (necessary substrate)
- Simulation physics (necessary substrate)
- Learning rate 0.08 (discovered via observation, then fixed)
- That SOME gradient exists (unavoidable in neural networks)

### What's Now "Environmental"
- Which optimizer each agent uses
- No human choosing "the best" optimizer

### Key Philosophy
This is **observation without intervention**. We're not selecting the best optimizer. We're not tuning learning rates. We're watching what happens when different physics-of-learning encounter the same environment.

## Changes Made

### Network.java
- Added `OptimizerType` enum: VANILLA, MOMENTUM, ADAM
- Added optimizer state (momentum vectors, Adam m/v)
- New constructor accepting optimizer type
- `learn()` dispatches to appropriate update rule

### Agent.java (new)
- Encapsulates one agent: world, network1, network2
- Random optimizer assigned at birth, fixed lr=0.08
- Tracks own performance statistics
- `runEpisode(seed)` - same seed = same landscape for fair comparison

### Simulation.java
- Creates 12 agents with random configurations
- Each episode: same seed for all agents
- Logs comparative performance
- Sorts agents by rolling average each log interval

## Expected Observations
- Some optimizer/LR combos will perform better
- But we're not selecting - just watching
- May see emergence of "naturally fit" configurations
- May observe stability differences between optimizers

## Actual Findings

### Learning Rate Discovery
During initial random LR testing, an agent with **lr=0.08** reached 50 avg quickly while others with lr=0.01 struggled. This led to fixing all agents at lr=0.08 - the higher rate works better for this simple task.

### Momentum Dominates
**Momentum consistently outperformed vanilla SGD and Adam.**

Hypothesis: Momentum's inertia helps it "roll through" small local minima. Since the landscape changes each episode, vanilla SGD over-reacts to transient gradients, and Adam's adaptive rates over-tune to noise. Momentum hits the sweet spot.

### Convergence Plateau
Best rolling average: ~50.7 after 3.4k episodes
Best single episode: 499 steps (near theoretical max of ~500)

The gap between peak (499) and average (50) suggests:
- Agent learned a strategy that works brilliantly on some landscapes
- But fails on others - hasn't generalized "read and navigate to safety"
- May be hitting limits of "physics teaches after the fact" approach

### Bug Found: Identical Initialization
All momentum agents showed **identical** best and rolling averages - suspicious. Root cause: `new Random()` in Network.java called in quick succession gave same time-based seed. All momentum agents had identical initial weights → identical learning trajectories.

**Fixed**: Network now seeds with `System.nanoTime() ^ Thread.currentThread().getId() ^ random`.

## Environment Evolution

### Center Spawn (Original)
Agents learned to **do nothing**. Passivity won - lucky green spawns outweighed red deaths. Not intelligence, just luck exploitation.

### Red Spawn (Worst Spot)
Changed environment to spawn on highest-decay spot. Forced movement to survive.

**Results:**
- Adam emerged as most consistent performer
- Momentum had highest peaks but inconsistent (commits hard, sometimes wrong)
- Vanilla middle ground

**New exploit found:** Agents navigate to walls, spam movement into wall = "free stay" without learning when to actually stay.

**Adam quirk:** Sometimes freezes on red at spawn. Also navigates perfectly to green... then walks off into red. Learned "movement is salvation" not "green is salvation."

---

## THE BREAKTHROUGH

### The Real Lesson

**The problem was never the agent. Never the optimizer. Never the learning rate. Never the architecture.**

**The problem is always the environment.**

The agent will *always* find the easiest path. If that path isn't intelligent, you get:
- Passivity (center spawn)
- Wall-hugging (red spawn)
- Every environment has its exploits

### Why Nature Succeeded

Nature's environment **self-corrects**:
- Exploit works → more agents use it → resources deplete → exploit fails
- No human intervention needed
- Agents ARE each other's environment
- Success automatically creates conditions for failure

Our simulation:
- Exploit works → works forever → nothing changes
- No competition, no arms race, no escalation

### Two Paths to AI

1. **Human-defined learning (Modern ML)**
   - Define right/wrong explicitly (rewards, labels, RLHF)
   - Fast, controllable, useful
   - Creates tools that serve human goals
   - Not emergence - directed optimization

2. **Genuine emergence**
   - No definitions, only environment physics
   - Trial and error on the *world*, not the agent
   - Success must automatically close the exploit
   - Slow, unpredictable, might fail entirely
   - But if it works - it's real

### Core Insight

**Emergence isn't found by setting up and waiting. It's engineered through the environment.**

The work isn't making smarter agents. It's designing environments where:
- The easiest path IS the intelligent path
- Exploits self-close without intervention
- "Good enough" keeps moving forever

**The environment is the code. Get that right, and emergence follows.**

---

## Running
```bash
cd src
javac *.java
java Simulation
```
