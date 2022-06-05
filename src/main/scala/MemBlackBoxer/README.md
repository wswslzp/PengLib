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

Users have to provide the necessary information of a memory vendor:
* How an SRAM of this vendor connects to the memory wrapper.
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

A memory wrapper (class `MemWrapper`) is a hardware module that connects the unified memory interface to the specific SRAM instance's ports,
which are vendor-specific. The memory wrapper modules are designed in [MemWrapper](src/main/scala/MemBlackBoxer/MemManager/MemWrapper.scala).

There are also four types of memory wrapper for each SRAM type, corresponding to four types of memory: 
1. single port SRAM
2. dual port SRAM
3. two port SRAM
4. ROM.

The memory wrapper contains a memory blackbox representing the true SRAM (ROM) instance named `ram`.
It's a SRAM instance of a specific memory vendor like `Huali`, which has Huali-specific memory ports. 
You usually create such memory instance using memory compiler (like Synopsys memory compiler `integrator` tool). 
The port definitions are contained in the generated Verilog/VHDL model. So in SpinalHDL, this 

This inner blackbox `ram` is created by `build` method of the `MemVendor`. 

And it will be built by passing `this` memory wrapper as parameter.

This `build` flow will not only create memory instance, but also handle the connection between SRAM ports and the unifed interface
(this is why it needs the reference to the wrapper).

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
