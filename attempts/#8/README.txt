ATTEMPT #8: Pure Physics - Single Agent, Fixed Learning Rate
Started: December 19, 2025
Ended: December 20, 2025

=== SETUP ===
- Single agent (no population)
- Continuous memory across deaths
- N1 (Body): 103 inputs -> [32, 16] -> 5 outputs
- N2 (Brain): 109 inputs -> [64, 32] -> 5 outputs
- Fixed learning rate: 0.01 (same for both, called "physics constant")
- Physics: delta is the only teacher
- No death signal, no counterfactual hints
- No evolution, no selection

=== PHILOSOPHY ===
physics -> N2 -> N1 -> actions -> physics

N2 learns from delta (physics consequence) only.
N1 learns from N2 only.
Pure emergence from physics alone.

=== RESULTS ===
- Ran for ~7+ million lives
- Baseline (random): 34 steps
- Floor rose from 35 to 36
- Peaks reached 38.8, occasional 39s
- Oscillation pattern: climbs to 38-39, falls back to 35-37
- Could not hold gains - good patterns get overwritten
- Best single episode: 171 steps

=== KEY OBSERVATIONS ===
1. Pure physics CAN teach (34 -> 39 is real improvement)
2. Single agent oscillates around local optimum
3. Local minima problem: agent finds good spot, can't hold it
4. No escape mechanism for getting off local peaks
5. Fixed gradient = human control we didn't need to exert

=== INSIGHTS (Philosophical Breakthroughs) ===

1. THE GRADIENT AS CONSTRAINT
   - Gradient descent is human-designed, but unavoidable
   - It's structure (HOW to learn), not goals (WHAT to learn)
   - We can't escape needing some learning mechanism

2. RELEASING CONTROL OF THE CONSTRAINT
   - If gradient is unavoidable, why control its form?
   - Randomize the optimizer per agent/life
   - Make learning physics environmental, not human-chosen

3. THE NEW IDEA: ENVIRONMENTAL LEARNING PHYSICS
   - Each agent gets randomly assigned optimizer
   - Vanilla gradient, momentum, Adam, different learning rates
   - The environment decides how each agent learns
   - Human provides substrate, environment provides everything else

4. POPULATION WITHOUT SELECTION
   - Multiple agents, same physics, no interaction
   - They don't know each other exist
   - No breeding, no selection, no designed fitness
   - Just parallel experiments with different random initializations
   - Different agents might find different peaks by luck

5. CLOSER TO PURE EMERGENCE
   Previously removed:
   - Goals/rewards (physics teaches)
   - Selection/evolution (no human fitness function)

   Now also removing:
   - Fixed optimizer choice (environment decides)

   What remains human-designed:
   - Network architecture (necessary scaffolding)
   - Simulation physics (necessary environment)
   - That SOME gradient exists (unavoidable)

=== LEARNING MOMENT ===
"The constraint exists - but we've stopped pretending to control it."

By randomizing the gradient method, we get:
- Exploration of optimizer space
- No human choice about "best" method
- Practical benefits of better gradients
- Philosophical purity maintained

=== NEXT EXPERIMENT ===
Attempt #9 should implement:
1. Multiple agents (population without selection)
2. Random optimizer assignment per agent
3. Each agent learns independently from physics
4. No sharing, no breeding, just parallel observation

=== LITERATURE CONNECTIONS ===
Started reading:
- Thomas Ray's Tierra (digital organisms, CPU as resource)
- Karl Sims' Evolved Virtual Creatures
- Varela's Embodied Mind (enactivism)
- Autopoiesis (Maturana & Varela)

This experiment is more radical than Tierra - no reproduction, no competition.
Just physics teaching individual networks in isolation.
