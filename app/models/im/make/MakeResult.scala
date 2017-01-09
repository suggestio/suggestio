package models.im.make

import io.suggest.ym.model.common.MImgSizeT
import models.im.MImgT
import models.mctx.Context
import play.api.mvc.Call

/**
 * При рендере bg по-широкому в шаблоне нужны данные по этой широкой картинке.
 * Эти параметры приходят из контроллера, т.к. для их сборки требуется асинхронный контекст (ибо работа с кассандрой).
 */
trait IMakeResult {

  /** Данные о размере картинки в css-пикселях, т.е. без учета плотности пикселей девайса. */
  def szCss         : MImgSizeT

  /** Данные о размере картинки в реальных экранных пикселях экрана устройства. Превышают szCss в szMult раз. */
  def szReal        : MImgSizeT

  /** Данные для сборки ссылки на картинку. */
  def dynCallArgs   : MImgT

  /** Является ли результат make - широкоформатным рендером? */
  def isWide        : Boolean

  def dynImgCall(implicit ctx: Context): Call = {
    ctx.api.dynImgUtil.imgCall(dynCallArgs)
  }

}


/** Дефолтовая реализация [[IMakeResult]]. */
case class MakeResult(
  override val szCss         : MImgSizeT,
  override val szReal        : MImgSizeT,
  override val dynCallArgs   : MImgT,
  override val isWide        : Boolean
)
  extends IMakeResult
