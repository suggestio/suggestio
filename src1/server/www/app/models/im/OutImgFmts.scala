package models.im

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.playx.FormMappingUtil
import japgolly.univeq.UnivEq
import play.api.mvc.QueryStringBindable

import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 17:46
 * Description: Файловые форматы изображений. Изначально были выходными форматами, но это в целом необязательно.
 * 2015.mar.13: Рефакторинг модели, добавление поддержки QueryStringBindable.
 */

case object OutImgFmts extends StringEnum[OutImgFmt] {

  case object JPEG extends OutImgFmt("jpeg") {
    override def mime = "image/jpeg"
  }

  case object PNG extends OutImgFmt("png") {
    override def mime = "image/png"
  }

  case object GIF extends OutImgFmt("gif") {
    override def mime = "image/gif"
  }

  case object SVG extends OutImgFmt("svg") {
    override def mime = "image/svg+xml"
  }


  override val values = findValues

  /**
   * Предложить формат для mime-типа.
   *
   * @param mime Строка mime-типа.
   * @return OutImgFmt. Если не-image тип, то будет IllegalArgumentException.
   */
  def forImageMime(mime: String): Option[OutImgFmt] = {
    values
      .find( _.mime.equalsIgnoreCase(mime) )
  }

}


sealed abstract class OutImgFmt(override val value: String) extends StringEnumEntry {

  def mime: String

  final def name = value

  override final def toString = value

}

object OutImgFmt {

  implicit def univEq: UnivEq[OutImgFmt] = UnivEq.derive

  implicit def outImgFmtQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[OutImgFmt] = {
    EnumeratumJvmUtil.valueEnumQsb( OutImgFmts )
  }

  def mappingOpt = EnumeratumJvmUtil.stringIdOptMapping( OutImgFmts )
  def mapping = FormMappingUtil.optMapping2required( mappingOpt )

}

