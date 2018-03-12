package models.im

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.img.ImgCropParsersImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.01.18 15:01
  * Description: ImOp codes model for url-qs.
  */

sealed abstract class ImOpCode(override val value: String) extends StringEnumEntry {
  def mkOp(vs: Seq[String]): ImOp
}


object ImOpCodes extends StringEnum[ImOpCode] {

  object AbsCrop extends ImOpCode("a") {
    override def mkOp(vs: Seq[String]) = {
      AbsCropOp( new ImgCropParsersImpl().apply(vs.head))
    }
  }
  object Gravity extends ImOpCode("b") {
    override def mkOp(vs: Seq[String]) = {
      ImGravities.withValue( vs.head )
    }
  }
  object AbsResize extends ImOpCode("c") {
    override def mkOp(vs: Seq[String]): ImOp = {
      AbsResizeOp(vs.head)
    }
  }
  object Interlace extends ImOpCode("d") {
    override def mkOp(vs: Seq[String]) = {
      ImInterlaces.withValue( vs.head )
    }
  }
  object GaussBlur extends ImOpCode("e") {
    override def mkOp(vs: Seq[String]) = {
      GaussBlurOp(vs.head.toInt)
    }
  }
  object Quality extends ImOpCode("f") {
    override def mkOp(vs: Seq[String]): ImOp = {
      QualityOp(vs.head.toInt)
    }
  }
  object Extent extends ImOpCode("g") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ExtentOp(vs.head)
    }
  }
  object Strip extends ImOpCode("h") {
    override def mkOp(vs: Seq[String]): ImOp = {
      StripOp
    }
  }
  object Filter extends ImOpCode("i") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ImFilters.withValue( vs.head )
    }
  }
  object SamplingFactor extends ImOpCode("j") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ImSamplingFactors.withValue( vs.head )
    }
  }
  //object PercentSzCrop extends ImOpCode("k") {
  //  override def mkOp(vs: Seq[String]): ImOp = {
  //    PercentSzCropOp(new ImgCropParsersImpl().apply(vs.head))
  //  }
  //}
  object Background extends ImOpCode("l") {
    override def mkOp(vs: Seq[String]): ImOp = {
      BackgroundOp( vs )
    }
  }

  override def values = findValues

}

