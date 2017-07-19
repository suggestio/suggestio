package io.suggest.sjs.common.vsz

import io.suggest.common.geom.d2.MSize2di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 18:28
 * Description: Кроссбраузерный доступ к размерам отображаемой области.
 * ViewportSzT -- аддон для сборки DirectRrr рендерера.
 */

trait IViewportSz {

  def widthPx : Option[Int]
  def heightPx: Option[Int]

  def getViewportSize: Option[MSize2di] = {
    for {
      w <- widthPx
      h <- heightPx
    } yield {
      MSize2di(width = w, height = h)
    }
  }

}


/** Аддон для готовой реализации детектора размера со всеми аддонами.
  * Используется для тестирования и для подмешивания в основную логику приложения. */
object ViewportSz
  extends IViewportSz
  with StdWndInnerSz
  with DocElSz
  with BodyElSz

