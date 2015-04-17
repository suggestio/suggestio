package models.im.make

import io.suggest.ym.model.common.MImgSizeT
import models.im.MImg
import play.api.mvc.Call
import util.img.DynImgUtil

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
  def dynCallArgs   : MImg
  /** Является ли результат make - широкоформатным рендером? */
  def isWide        : Boolean

  def dynImgCall    : Call = {
    DynImgUtil.imgCall(dynCallArgs)
  }
}


/** Дефолтовая реализация [[IMakeResult]]. */
case class MakeResult(
  szCss         : MImgSizeT,
  szReal        : MImgSizeT,
  dynCallArgs   : MImg,
  isWide        : Boolean
) extends IMakeResult
