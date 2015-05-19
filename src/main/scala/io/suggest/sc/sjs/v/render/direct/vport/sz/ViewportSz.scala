package io.suggest.sc.sjs.v.render.direct.vport.sz

import io.suggest.adv.ext.model.im.{Size2di, ISize2di}
import io.suggest.sc.sjs.v.render.IRenderer

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

  def getViewportSize: Option[ISize2di] = {
    for {
      w <- widthPx
      h <- heightPx
    } yield {
      Size2di(width = w, height = h)
    }
  }

}


/** Аддон для готовой реализации детектора размера со всеми аддонами.
  * Используется для тестирования и для подмешивания в основную логику приложения. */
trait ViewportSzImpl
  extends IViewportSz
  with StdWndInnerSz
  with DocElSz
  with BodyElSz


/** Аддон для DirectRrr, добавляющий поддержку чтения размеров viewport из нескольких источников.
  * Можно подключать новые и отключать текущие модули чтения размера на стадии компиляции. */
trait ViewportSzT
  extends ViewportSzImpl
  with IRenderer
{
  override def viewportSize: ISize2di = getViewportSize.get
}
