package io.suggest.sc.sjs.v.render.direct

import io.suggest.sc.sjs.v.render.IRenderer
import io.suggest.sc.sjs.v.render.direct.index.ShowIndex
import io.suggest.sc.sjs.v.render.direct.vport.sz.ViewportSzT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 18:01
 * Description: Реализация прямого рендеринга в DOM через javascript-методы DOM и window.
 */
class DirectRrr
  extends IRenderer
  with ViewportSzT
  with ShowIndex

