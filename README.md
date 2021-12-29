# PengLib

Collecting some infrastructure library in SpinalHDL. The feature of this library includes:

* Customizable SRAM converter
* Automatic Ping pong wrapper
* linear interpolation package
* and others...

## SRAM converter

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
  val vendor =MemBlackBoxer.MemManager.Huali
  SpinalConfig(
    targetDirectory = "rtl",
    memBlackBoxers = mutable.ArrayBuffer(new PhaseSramConverter(vendor))
  ).generateVerilog(MemToy())
}
```
Inside the `MemToy`, a module named `Axi4SharedOnChipRam` is instantiated as `mem`. A `Mem` object `ram` is inside the module,
and it will be generated as an register array like `reg [31:0] ram[512];`.

If we want to generate an SRAM instead of a Verilog register array, we can pass a SRAM converter
`PhaseSramConverter` to `SpinalConfig` as above. You don't have to change any line of your original RTL source code (sure you cannot change the 
implementation of the class in `spinal.lib`).

The `PhaseSramConverter` takes a parameter that specifies the SRAM vendor. It depends on which 
memory compiler you are using. Here as an example the `Huali` memory compiler is used. 

Users have to provide the necessary information of a vendor:
* How an SRAM of the vendor connects to the memory wrapper.
* Which memory topology is allowed, single port / dual port / two port ...
* The naming convention of an SRAM.

The following steps are **mandatory**:
1. Implement the memory blackbox by extending `SinglePortBB`/`DualPortBB`/`TwoPortBB`/`RomBB`. Declare the SRAM port and connect them to memory wrapper in `build()`.
2. Implement the vendor object by extending `MemVendor`. Provide the prefix of the name or even use your own name convention. Implement corresponding `build()` function.

As an example, the `Huali` vendor object and its SRAM blackboxes are already included in [MemVendor](src/main/scala/MemBlackBoxer/MemManager/MemVendor.scala) and [Vendor.Huali](src/main/scala/MemBlackBoxer/Vendor/Huali).

The default memory blackboxer policy used in `MemVendor` is `blackboxAll`, implemented in `spinal.core`. You can change the default policy to change the way in which the SRAM is blackboxed. 

There are still some features todo:
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
