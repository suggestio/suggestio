package models.adr

import models.im._
import models.{ImgCrop, MImgSizeT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 17:43
 * Description: Модель расширенных параметров для вызова рендера карточки через wkhtml2image.
 */

object MWkHtmlArgs {
  def ZOOM_OPT_DFLT     : Option[Float]     = None
  def WITH_PLUGINS_DFLT : Boolean           = false
  def CROP_DFLT         : Option[ImgCrop]   = None
  def SMART_WIDTH_DFLT  : Boolean           = true
}


/** Интерфейс расширенных аргументов для вызова wkhtml2image. */
trait IWkHtmlArgsT extends IAdRenderArgs {

  /** Отмасштабировать страницу? */
  def zoomOpt : Option[Float]

  /** Разрешить браузерные плагины? */
  def plugins : Boolean

  /** Необязательный кроп. */
  def crop    : Option[ImgCrop]

  /** Разрешать ли wkhtml переопределять заданную ширину? Нужно patched Qt version installed. */
  def smartWidth: Boolean

}

/** Трейт с дефолтовыми значениями расширенных параметров вызова wkhtml. */
trait IWhHtmlArgsDflt extends IWkHtmlArgsT {
  import MWkHtmlArgs._
  override def zoomOpt     : Option[Float]     = ZOOM_OPT_DFLT
  override def plugins     : Boolean           = WITH_PLUGINS_DFLT
  override def crop        : Option[ImgCrop]   = CROP_DFLT
  override def smartWidth  : Boolean           = SMART_WIDTH_DFLT
}


/** Реализация расширенных аргументов для вызова wkhtml.
  * На момент написания -- не использовалась, просто сюда запихнут возможно нужный в будущем
  * расширенный набор аргументов, уже реализованных к моменту DI-рефакторинга.
  */
case class MWkHtmlArgs(
  override val src         : String,
  override val scrSz       : MImgSizeT,
  override val outFmt      : OutImgFmt,
  override val quality     : Option[Int],
  override val zoomOpt     : Option[Float]     = MWkHtmlArgs.ZOOM_OPT_DFLT,
  override val plugins     : Boolean           = MWkHtmlArgs.WITH_PLUGINS_DFLT,
  override val crop        : Option[ImgCrop]   = MWkHtmlArgs.CROP_DFLT,
  override val smartWidth  : Boolean           = MWkHtmlArgs.SMART_WIDTH_DFLT
)
  extends IWkHtmlArgsT

