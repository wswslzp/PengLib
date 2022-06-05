## Improved Netlist API

The original SpinalHDL netlist API is not convenient enough 
to analyze the circuit structure. This library provide two 
analyzers: `ModuleAnalyzer` and `DataAnalyzer` to analyze the 
module and the signal inside, implementing a better netlist API.

### Module Analyzer

The `ModuleAnalyzer` provides some commands similar to the SDC 
commands(tcl left, scala right):

* `all_clocks` <--> `allClocks`, return all embedding clock domains.
* `all_inputs` <--> `allInputs`, return all the input pins.
* `all_outputs`<--> `allOutputs`, return all the output pins.
* `all_registers` <--> `allRegisters`, return all the regs.
* `get_pins` <--> `getPins`, return pins according to some conditions.
* `get_ports` <--> `getPorts`, return top level module's port according to some conditions.
* `get_nets` <--> `getNets`, return the wire 
* `get_cells` <--> `getCells`, return sub module instances
* `get_lib_cells` <--> `getLibCells`, return sub blackbox instances
* `get_lib_pins` <--> `getLibPins`, return sub blackbox instances' pins

### Data Analyzer

The `DataAnalyzer` provides two function to get all fan-in signals and fan-out signals of
a signal:

* `allFanIn` returns a set of all the fan-in signals
* `allFanOut` returns a set of all the fan-out signals

Also provides two conditional accessing methods: 

* `getFanIn(cond: BaseType=> Boolean)` conditionally collect fan-in signals
* `getFanOut(cond: BaseType=> Boolean)` conditionally collect fan-out signals

And two walking function to run a customized function on every fan-in or fan-out signals.

* `walkFanIn(cond: BaseType=> Boolean)(func: BaseType=> Unit)`
* `walkFanOut(cond: BaseType=> Boolean)(func: BaseType=> Unit)`
