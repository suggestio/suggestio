package models.im

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.playx.FormMappingUtil
import japgolly.univeq.UnivEq

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 14:25
 * Description: Программы компрессии, используемые при сохранении картинок.
 */
case object CompressModes extends StringEnum[CompressMode] {

  /** Компрессия фона. */
  case object Bg extends CompressMode("bg") {
    override def fromDpr(dpr: DevPixelRatio): ImCompression = {
      dpr.bgCompression
    }
    override def nameI18n = "Background"
  }

  /** Компрессия для изображений переднего плана. */
  case object Fg extends CompressMode("fg") {
    override def fromDpr(dpr: DevPixelRatio): ImCompression = {
      dpr.fgCompression
    }
    override def nameI18n = "Foreground"
  }

  override val values = findValues

}


sealed abstract class CompressMode(override val value: String) extends StringEnumEntry {

  def nameI18n: String

  def fromDpr(dpr: DevPixelRatio): ImCompression

}

object CompressMode {

  implicit def univEq: UnivEq[CompressMode] = UnivEq.derive

  def mappingOpt = EnumeratumJvmUtil.stringIdOptMapping( CompressModes )
  def mapping = FormMappingUtil.optMapping2required( mappingOpt )

}
