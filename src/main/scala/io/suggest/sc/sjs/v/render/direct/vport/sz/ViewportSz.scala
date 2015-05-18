package io.suggest.sc.sjs.v.render.direct.vport.sz

import io.suggest.adv.ext.model.im.ISize2di
import io.suggest.sc.sjs.v.render.IRenderer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 18:28
 * Description: Кроссбраузерный доступ к размерам отображаемой области.
 * ViewportSzT -- аддон для сборки DirectRrr рендерера.
 */

trait IViewportSz {
  def getViewportSize: Option[ISize2di]
}


/** Аддон для DirectRrr, добавляющий поддержку чтения размеров viewport из нескольких источников.
  * Можно подключать новые и отключать текущие модули чтения размера на стадии компиляции. */
trait ViewportSzT
  extends IViewportSz
  with IRenderer
  with StdWndInnerSz
  with DocElSz
  with BodyElSz
{
  override def viewportSize: ISize2di = getViewportSize.get
}
