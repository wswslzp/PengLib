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

// point to the PengLib directory
lazy val penglib = ProjectRef(file("../PengLib"), "penglib")

lazy val root = (project in file(".")).
  settings(
    name := "usepeng",
    libraryDependencies ++= spinal,
    fork := true,
    javaOptions := Seq("-Xmx16G")
  ).dependsOn(penglib) // remember adding dependency on penglib
```

Then your project now is depending on `PengLib`. Also you can download this repo and publish local `jar` files then use the jar library in your project as well.

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
