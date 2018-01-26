package models.im

import enumeratum.values.{StringEnum, StringEnumEntry}
import org.im4java.core.IMOperation

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.01.18 15:08
  * Description:ImageMagick gravities model.
  */

sealed abstract class ImGravity(override val value: String) extends StringEnumEntry with ImOp {
  def imName: String

  override def opCode = ImOpCodes.Gravity
  override def addOperation(op: IMOperation): Unit = {
    op.gravity(imName)
  }
  override def qsValue = value
  override def unwrappedValue = Some(imName)
}


object ImGravities extends StringEnum[ImGravity] {

  // Некоторые значения помечены как lazy, т.к. не используются по факту.
  object Center extends ImGravity("c") {
    override def imName = "Center"
  }

  // Не используются в проекте - скрыты.
  /*
  object North extends ImGravity("n") {
    override def imName = "North"
  }
  object South extends ImGravity("s") {
    override def imName = "South"
  }
  object West extends ImGravity("w") {
    override def imName = "West"
  }
  object East extends ImGravity("e") {
    override def imName = "East"
  }
  */

  override def values = findValues

}

