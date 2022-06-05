# PengLib

Collecting some infrastructure library in SpinalHDL. The feature of this library includes:

* Customizable SRAM converter
* Automatic Ping pong wrapper
* Some mathematics libraries
* and others...

## Usage

1. create your own SpinalHDL project.

```shell
sbt new wswslzp/spinal.g8
```

2. clone this project in the same directory as your project resides.

```shell
git clone git@github.com:wswslzp/PengLib.git
```

3. configure your `build.sbt` in your own project as follows

```scala
// give the user a nice default project!
ThisBuild / organization := "com.github.wswslzp"
ThisBuild / scalaVersion := "2.12.13"
ThisBuild / version := "v0.1"

val spinalVersion = "1.7.0"
val spinal = Seq(
  "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion,
  "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion,
  compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)
)

lazy val penglib = ProjectRef(file("../PengLib"), "penglib")

lazy val root = (project in file(".")).
  settings(
    name := "usepeng",
    libraryDependencies ++= spinal,
    fork := true,
    javaOptions := Seq("-Xmx16G")
  ).dependsOn(penglib)
```

Then your project now is depending on `PengLib`. Also you can download this repo and publish local `jar` files then use the jar library in your project as well.

## SRAM converter

### Overview

We can insert a SpinalHDL phase to convert the `Mem` into vendor-specific SRAM.
For example, we have a module `MemToy` as follows:
```scala
import spinal.core._
import spinal.lib._
import bus.amba4.axi._
import java.io._
import scala.collection.mutable
import MemBlackBoxer.PhaseMemBlackBoxer._

case class MemToy() extends Component {
  // a axi4 shared bus interface memory, 
  // with data width 32 bit, 512 depth and id width of 5
  val mem = Axi4SharedOnChipRam(32, 512, 5)
  val bus = slave(Axi4Shared(mem.axiConfig))
  mem.io.axi <> bus
}
def main(args: Array[String]): Unit = {
  new File("rtl").mkdir()
  val vendor = MemBlackBoxer.Vendor.Huali
  val config = SpinalConfig(
    targetDirectory = "rtl"
  )
  config.addTransformationPhase(
    new PhaseSramConverter(vendor, policy = blackboxAll)
  )
  config.generateVerilog(MemToy())
}
```
Inside the `MemToy`, a module named `Axi4SharedOnChipRam` is instantiated as `mem`. A `Mem` object `ram` is inside the module,
and it will be generated as an register array like `reg [31:0] ram[512];`.

If we want to generate an SRAM instead of a Verilog register array, 
we can pass a SRAM converter `PhaseSramConverter` to `SpinalConfig` as above,
or use `addTransformationPhase` method of `SpinalConfig`.
You don't have to change any line of your original RTL source code 
(sure you cannot change the implementation of the class in `spinal.lib`).

The `PhaseSramConverter` takes a parameter that specifies the SRAM vendor. It depends on which 
memory compiler you are using. Here as an example the `Huali` memory compiler is used. 

Users have to provide the necessary information of a vendor:
* How an SRAM of the vendor connects to the memory wrapper.
* Which memory topology is allowed, single port / dual port / two port ...
* The naming convention of an SRAM.

The following steps are **mandatory**:
1. Implement the memory blackbox by extending `SinglePortBB`/`DualPortBB`/`TwoPortBB`/`RomBB`. Declare the SRAM port and connect them to memory wrapper in `build()`.
2. Implement the vendor object by extending `MemVendor`. Provide the prefix of the name or even use your own name convention. Implement corresponding `build()` function.

As an example, the `Huali` vendor object and its SRAM blackboxes are already included in 
[MemVendor](src/main/scala/MemBlackBoxer/MemManager/MemVendor.scala) and 
[Vendor.Huali](src/main/scala/MemBlackBoxer/Vendor/Huali.scala).

The default memory blackboxer policy used in `MemVendor` is `blackboxAll`, implemented in `spinal.core`. You can change the default policy to change the way in which the SRAM is blackboxed. 

### Memory Wrapping Flow

To provide a unified memory interface, this memory converter creates memory wrappers for all the `Mem` instance. 
You can also use the `MemWrapper` feature directly, without inserting memory blackboxing phases.

A memory wrapper (class of `MemWrapper`) is a hardware module that connects the unified memory interface to the specific SRAM instance's ports,
which are various for different memory vendor. 
It contains a memory blackbox representing the true SRAM instance named `ram`.

This inner blackbox `ram` is created by `build` method of the `MemVendor`. 
It's a SRAM instance of a specific memory vendor like `Huali`.
And it will be built by passing `this` memory wrapper as parameter.

This `build` flow will not only create memory instance, but also handle the connection between SRAM ports and the unifed interface
(this is why it needs the reference to the wrapper).

Because there are usually four types of memory: single port SRAM, dual port SRAM, two port SRAM and ROM,
there are also four types of memory wrapper for each SRAM type.

As in [Huali.build()](src/main/scala/MemBlackBoxer/Vendor/Huali.scala),

### Memory Vendor System

For memory vendor, you need to provide a specific SRAM vendor object that extends `MemVendor` trait. 

The example for `Huali` vendor has been shown  as above.

There are two mandatory methods you have to implement, as above discussed.
And four type of memory hardware model should be implemented.

**Build** 

`build` function has a parameter of type `MemWrapper`. 
`build` function check which memory type the wrapper is and create the corresponding memory blackbox that represents the SRAM hardware model.

This method should be simplified because for all memory vendors it will always seem to be the same.

**Prefix**

A prefix name should be provided. 

**Memory Blackbox**

`build` function builds four types of memory. 
So you have to implement these four `Blackbox` of memory in the vendor `object`.
The memory blackboxes extends a specific memory type, as following:
* `SinglePortBB` - For single port SRAM, one read-write port.
* `DualPortBB` - For dual port SRAM, one read-only port and one write-only port.
* `TwoPortBB` - For two port SRAM, two read-write ports.
* `RomBB` - For ROM.

You don't have actually to implement all these four types, 
or you can implement some memory type that doesn't even exist as above.
It depends on how you implement the `build` function of the vendor `object`.

### Memory Tags

You can specify different memory vendors in your design. 
In some cases, you have several `Mem` instances but some of them  
belong to vendor A, some belong to B and some should be pure register file.

Then you can change the default memory blackbox policy `blackboxAll` to following policies:
* `blackboxAllWithVendorTag`, the policy that will not blackbox the memory by default, except for tagged memory.
* `blackboxAllWithoutUnusedTag`, the policy that will blackbox all the memory by default, except for those tagged with `dontBB`.

The policy can be passed to `PhaseSramConverter` as a parameter.

You can tag the memory with a specific `MemVendor`. 

### TODO

There are still some features todo:
* Improve `MemVendor` trait, like move `build` method up to `MemVendor`.
* The connection of the MBIST port of the SRAM.
* The BIST logic implementation. 
* The low power port connection.

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

## Math library

The math lib lies in `src/main/MathLib`. Two basic number types
are included - Fixed number and Complex number. The Fixed number 
here is an extension of `spinal` built-in `SFix` and `UFix`. And 
the Complex number `HComplex` is implemented as a bundle, combining
real part and imagine part of `SFix`. The basic operations like 
+/-/*/div/conj etc., are implemented as methods. 

Some math functions like `sqrt` are also implemented based on these two number types.

Fast Fourier Transformation (FFT) is also implemented, both for 
1D and 2D, with fully configurable feature.

Several interpolation methods are also included, such as nearest interpolation,
linear interpolation, bi-linear interpolation. 

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
