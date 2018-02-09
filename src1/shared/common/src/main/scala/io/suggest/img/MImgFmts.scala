package io.suggest.img

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.12.14 17:46
  * Description: Файловые форматы картинок.
  * Изначально были выходными форматами, но стали просто форматами.
  */

case object MImgFmts extends StringEnum[MImgFmt] {

  case object JPEG extends MImgFmt("jpeg") {
    override def mime = "image/jpeg"
  }

  case object PNG extends MImgFmt("png") {
    override def mime = "image/png"
  }

  case object GIF extends MImgFmt("gif") {
    override def mime = "image/gif"
  }

  case object SVG extends MImgFmt("svg") {
    override def mime = "image/svg+xml"
  }


  override val values = findValues

  /**
   * Предложить формат для mime-типа.
   *
   * @param mime Строка mime-типа.
   * @return OutImgFmt. Если не-image тип, то будет IllegalArgumentException.
   */
  def forImageMime(mime: String): Option[MImgFmt] = {
    values
      .find( _.mime.equalsIgnoreCase(mime) )
  }


  final def default: MImgFmt = JPEG

}


sealed abstract class MImgFmt(override val value: String) extends StringEnumEntry {

  def mime: String

  final def name = value

  override final def toString = value

}

object MImgFmt {

  implicit def univEq: UnivEq[MImgFmt] = UnivEq.derive

  implicit def OUT_IMG_FMT_FORMAT: Format[MImgFmt] = {
    EnumeratumUtil.valueEnumEntryFormat( MImgFmts )
  }

}
