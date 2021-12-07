# PengLib

Collecting some infrastructure library in SpinalHDL. The feature of this library includes:

* Customizable SRAM converter
* Automatic Ping pong wrapper
* linear interpolation package

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
Inside the `MemToy`, a module named `Axi4SharedOnChipRam` is instantiated as `mem`. A `Mem` object `ram` is inside the module.
If we want to generate an SRAM instead of a Verilog register array, we can pass a SRAM converter
`PhaseSramConverter` to `SpinalConfig` as above. 

The `PhaseSramConverter` takes a parameter that specifies the SRAM vendor. It depends on which 
memory compiler you are using. Here as an example the `Huali` memory compiler is used. 

Users have to provide the necessary information of a vendor:
* How an SRAM of the vendor connects with the memory wrapper.
* Which memory topology is allowed, single port / dual port / two port ...
* The naming convention of an SRAM.

The following steps are **mandatory**:
1. Implement the memory blackbox by extending `SinglePortBB`/`DualPortBB`/`TwoPortBB`/`RomBB`. Declare the SRAM port and connect them to memory wrapper in `build()`.
2. Implement the vendor object by extending `MemVendor`. Provide the prefix of the name or even use your own name convention. Implement corresponding `build()` function.

As an example, the `Huali` vendor object and its SRAM blackboxes are already included in [MemVendor](src/main/scala/MemBlackBoxer/MemManager/MemVendor.scala) and [Vendor.Huali](src/main/scala/MemBlackBoxer/Vendor/Huali).

The default memory blackboxer policy used in `MemVendor` is `blackboxAll`, implemented in `spinal.core`. You can change the default policy to change the way in which the SRAM is blackboxed. 