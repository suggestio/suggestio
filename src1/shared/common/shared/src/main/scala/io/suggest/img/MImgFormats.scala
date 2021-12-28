package io.suggest.img

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.12.14 17:46
  * Description: Файловые форматы картинок.
  * Изначально были выходными форматами, но стали просто форматами.
  */

case object MImgFormats extends StringEnum[MImgFormat] {

  private final val bInKb = 1024

  case object JPEG extends MImgFormat("j") {
    override def name = "jpeg"
    override def uploadMaxFileSizeKb = 30 * bInKb
    override def uploadSideSizeMaxPx = 8000
  }

  case object PNG extends MImgFormat("p") {
    override def name = "png"
    override def uploadMaxFileSizeKb = 5 * bInKb
    override def uploadSideSizeMaxPx = 5000
  }

  case object GIF extends MImgFormat("g") {
    override def name = "gif"
    override def imCoalesceFrames = true
    /** @see [[http://www.imagemagick.org/Usage/anim_opt/#optimize]] General Purpose GIF Optimizer of ImageMagick */
    override def layersOptimize = true
    override def imFinalRepage = true
    override def uploadMaxFileSizeKb = PNG.uploadMaxFileSizeKb
    override def uploadSideSizeMaxPx = 3000
  }

  /** SVG бывает SVGZ (пожатый GZIP), и просто голым текстом (SVG).
    * Тут как бы неявно унифицируются эти два варианта.
    */
  case object SVG extends MImgFormat("s") {
    override def name = "svg"
    def mimePrefix = super.mime
    override final def mime = mimePrefix + "+xml"

    override def otherMimes: List[String] = {
      (mime + "-compressed") ::
        Nil
    }

    /** Пусть на выходе convert'а по возможности будет пожатый SVG, попробуем заставить это работать. */
    override final def imFormat = imFormatNonCompressed + "Z"
    final def imFormatNonCompressed = super.imFormat

    override def uploadMaxFileSizeKb = 2 * bInKb
    override def uploadSideSizeMaxPx = 10000
  }


  override val values = findValues

  def allMimesIter: Iterator[String] = {
    values
      .iterator
      .flatMap(_.allMimes)
  }

  private val _mimeValues: Map[String, MImgFormat] = {
    (for {
      imgFmt <- values.iterator
      mime   <- imgFmt.allMimes
    } yield {
      mime.toLowerCase -> imgFmt
    })
      .toMap
  }

  /**
   * Предложить формат для mime-типа.
   *
   * @param mime Строка mime-типа.
   * @return OutImgFmt. Если не-image тип, то будет IllegalArgumentException.
   */
  def withMime(mime: String): Option[MImgFormat] = {
    // Не приводим mime к нижнему регистру. В проекте везде mime регистр должен быть нижним.
    _mimeValues.get(mime)
  }

  /** Поиск по файловому расширению (без точки, маленькими буквами). */
  def withFileExt(fileExt: String): Option[MImgFormat] = {
    // Типов немного, поэтому находим простым перебором.
    values.find(_.fileExt ==* fileExt)
  }


  final def default: MImgFormat = JPEG


  private def _nameLenghts = values.iterator.map(_.name.length)

  /** Минимальная длина полного названия формата. */
  def NAME_LEN_MIN = _nameLenghts.min

  /** Максимальная длина полного названия формата. */
  def NAME_LEN_MAX = _nameLenghts.max

}


/** Один элемент модели форматов изображений.
  *
  * @param value Сериализуемый идентификатор.
  *              Обычно строка в один символ, чтобы минимизировать объёмы JSON.
  */
sealed abstract class MImgFormat(override val value: String) extends StringEnumEntry {

  /** Название, фигурирующее почти везде. Только lower case. */
  def name: String

  def mime: String = "image/" + name

  override final def toString = name

  /** Префикс формата imagemagick в ВЕРХНЕМ регистре.
    *
    * Будет использован как префикс файла:
    * convert JPG:/path/to/input.jpeg PNG:/path/to/output.png
    *
    * @see convert -list format
    */
  def imFormat: String = name.toUpperCase

  /** other mimes -- вторичные mime-типы.
    * Т.е. возможно, что за каким-то img-форматом закреплено несколько MIME-типов.
    * Тогда, вторичные MIME-типы можно перечислить здесь.
    */
  def otherMimes: List[String] = Nil

  /** Добавлять -coalesce перед convert-операциями?
    * Требуется для анимированных форматов, чтобы между фреймами всё эффективно жалось.
    */
  def imCoalesceFrames: Boolean = false

  /** Бывает, что нужно делать +repage после обработки, но перед сохранением.
    * Для GIF'а например, из-за внутренних особенностей формата и его обработки.
    */
  def imFinalRepage: Boolean = false

  /** Добавлять -layers  Optimize */
  def layersOptimize: Boolean = false

  /** Аплоад: максимальный размер файла. */
  def uploadMaxFileSizeKb: Int

  /** Аплоад: максимальный размер одной стороны картинки. */
  def uploadSideSizeMaxPx: Int

}


object MImgFormat {

  @inline implicit def univEq: UnivEq[MImgFormat] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def OUT_IMG_FMT_FORMAT: Format[MImgFormat] = {
    EnumeratumUtil.valueEnumEntryFormat( MImgFormats )
  }


  implicit final class MImgFmtOpsExt( val imgFmt: MImgFormat ) extends AnyVal {

    /** Файловое расширение (без точки). Обычно эквивалентно name. */
    def fileExt: String =
      imgFmt.name

    /** Все связанные MIME-типы в произвольном порядке. */
    def allMimes: List[String] =
      imgFmt.mime :: imgFmt.otherMimes

    def uploadMaxFileSizeB: Int =
      imgFmt.uploadMaxFileSizeKb * 1024

    /** Является ли формат растровым? */
    def isRaster: Boolean =
      !isVector

    /** Является ли формат векторным? */
    def isVector: Boolean =
      imgFmt ==* MImgFormats.SVG

  }

}
