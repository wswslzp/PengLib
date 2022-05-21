import spinal.core._

package object NoC {

  trait RenameFlitPayload {this: Component=>
    noIoPrefix()
    afterElaboration {
      getAllIo.foreach(bt=> {
        if(bt.getName().contains("_payload_")) bt.setName(bt.getName().replace("_payload_", ""))
      })
    }
  }

}
