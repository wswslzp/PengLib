## Ping pong 

This library implement an automatic ping pong feature, 
enabled duplicating a pure combination logic module multiple instances
to make 'pingpong' on it, with same interface as the original module.

Typical usage as `PPP` module in [Ping Pong example](src/test/scala/PPTest.scala).

Function `PingPongComb(4)(PP())`, creates an `Area`. 
In this area, a new module that has same interfaces as the 
original module `PP` is instantialized. The new module's creation flow
is as follows: 
1. `cleanUp` the internal logic of the original module while keeping the interfaces unchanged and unconnected.
2. add the de-mux logic, including a buffer storing all input data, and the de-mux it to the datapath.
3. duplicate the original module `dc` times, composing the datapath.
4. add the mux logic, propagate the selecting signal to select the output from datapath.

In fact, the new module is derived from the original module. 
Therefore, we set a new name to the new module, and `rework` on it to rewrite the internal logic.
Note that the logic design implemented in the `rework` function has to reside in 
`AreaRoot` to give them name.

Currently, the module parameter is restricted to be a pure combination module.
We assume the module have a combination path that take more than one cycle (multi-cycle path),
and we use this feature to enable catching one output per cycle, like normal single-cycle path.
The `PingPongComb` will check if the module is pure combination.

Non-blocking data transfer protocol `Flow` and blocking protocol
`Stream` are not supported. Adding a `valid` signal to the input 
is not complicated while back-pressured `ready` is.
