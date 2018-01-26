package models.im

import enumeratum.values.{StringEnum, StringEnumEntry}
import org.im4java.core.IMOperation

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.01.18 15:39
  * Description: Im Interlaces model.
  */

sealed abstract class ImInterlace(override val value: String) extends StringEnumEntry with ImOp {
  def imName: String


  override final def qsValue = value
  override def opCode = ImOpCodes.Interlace
  override def addOperation(op: IMOperation): Unit = {
    op.interlace(imName)
  }
  override def unwrappedValue: Option[String] = Some(imName)
}


object ImInterlaces extends StringEnum[ImInterlace] {

  object Plane extends ImInterlace("a") {
    override def imName = "Plane"
  }

  // Not used:
  /*
  object None extends ImInterlace("0") {
    override def imName = "None"
  }
  object Line extends ImInterlace("l") {
    override def imName = "Line"
  }
  object Jpeg extends ImInterlace("j") {
    override def imName = "JPEG"
  }
  object Gif extends ImInterlace("g") {
    override def imName = "GIF"
  }
  object Png extends ImInterlace("p") {
    override def imName = "PNG"
  }
  object Partition extends ImInterlace("r") {
    override def imName = "Partition"
  }
  */

  override def values = findValues

}

