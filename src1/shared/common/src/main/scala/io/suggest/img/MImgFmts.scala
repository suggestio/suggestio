package io.suggest.img

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.common.html.HtmlConstants
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
    override final def mime = "image/jpeg"
  }

  case object PNG extends MImgFmt("png") {
    override final def mime = "image/png"
  }

  case object GIF extends MImgFmt("gif") {
    override final def mime = "image/gif"
  }

  /** SVG бывает SVGZ (пожатый GZIP), и просто голым текстом (SVG).
    * Тут как бы неявно унифицируются эти два варианта.
    */
  case object SVG extends MImgFmt("svg") {
    override final def mime = "image/svg+xml"
    /** Пусть на выходе convert'а по возможности будет пожатый SVG, попробуем заставить это работать. */
    override final def imageMagickFormat = imageMagickFormatNonCompressed + "Z"
    final def imageMagickFormatNonCompressed = super.imageMagickFormat
  }


  override val values = findValues

  val mimeValues: Map[String, MImgFmt] = {
    val iter = for {
      imgFmt <- values.iterator
      mime   <- imgFmt.allMimes
    } yield {
      mime.toLowerCase -> imgFmt
    }
    iter.toMap
  }

  /**
   * Предложить формат для mime-типа.
   *
   * @param mime Строка mime-типа.
   * @return OutImgFmt. Если не-image тип, то будет IllegalArgumentException.
   */
  def forImageMime(mime: String): Option[MImgFmt] = {
    // Не приводим mime к нижнему регистру. В проекте везде mime регистр должен быть нижним.
    mimeValues.get(mime)
  }


  final def default: MImgFmt = JPEG

}


/**  */
sealed abstract class MImgFmt(override val value: String) extends StringEnumEntry {

  def mime: String

  final def name = value

  override final def toString = value

  /** Префикс формата imagemagick в ВЕРХНЕМ регистре.
    *
    * Будет использован как префикс файла:
    * convert JPG:/path/to/input.jpeg PNG:/path/to/output.png
    *
    * @see convert -list format
    */
  def imageMagickFormat: String = value.toUpperCase

  /** other mimes -- вторичные mime-типы.
    * Т.е. возможно, что за каким-то img-форматом закреплено несколько MIME-типов.
    * Тогда, вторичные MIME-типы можно перечислить здесь.
    */
  def otherMimes: List[String] = Nil

  /** Все связанные MIME-типы в произвольном порядке. */
  final def allMimes: List[String] = mime :: otherMimes

  /** Файловое расширение. */
  def fileExt = value

}


object MImgFmt {

  implicit def univEq: UnivEq[MImgFmt] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def OUT_IMG_FMT_FORMAT: Format[MImgFmt] = {
    EnumeratumUtil.valueEnumEntryFormat( MImgFmts )
  }

}
