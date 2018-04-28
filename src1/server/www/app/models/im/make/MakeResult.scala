package models.im.make

import io.suggest.common.geom.d2.MSize2di
import models.im.MImgT
import models.mctx.Context
import play.api.mvc.Call

/** Модель данных по подготовке одной картинки.
  *
  * При рендере картинок под карточки в шаблоне нужны развёрнутые данные по этой картинке.
  * Эти параметры приходят из контроллера, т.к. для их сборки требуется асинхронный контекст.
  *
  * @param szCss Данные о размере картинки в css-пикселях, т.е. без учета плотности пикселей девайса.
  * @param szReal Данные о размере картинки в реальных экранных пикселях экрана устройства. Превышают szCss в szMult раз.
  * @param dynCallArgs Данные для сборки ссылки на картинку.
  * @param isWide Является ли результат make - широкоформатным рендером?
  * @param isFake Это был фейковый мейк.
  *               Например, svg не надо готовить, но результат всё равно нужен.
  *               И тогда isFake = true, т.е. ничего по факту не делалось.
  */
case class MakeResult(
                       szCss         : MSize2di,
                       szReal        : MSize2di,
                       dynCallArgs   : MImgT,
                       isWide        : Boolean,
                       isFake        : Boolean = false
                     ) {

  // TODO Удалить этот метод, он не модельный.
  def dynImgCall(implicit ctx: Context): Call = {
    ctx.api.dynImgUtil.imgCall(dynCallArgs)
  }

}
