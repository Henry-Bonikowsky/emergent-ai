ATTEMPT #4
==========
Physics-grounded learning + continuous memory.

- Removed mutation entirely
- N2 learns from physics (delta), N1 learns from N2
- Memory persists across deaths (no weight reset)
- Death teaches with full weight
- BUT: N2 was blind (only saw N1 output + delta, not state)
- Stuck at random baseline ~34
