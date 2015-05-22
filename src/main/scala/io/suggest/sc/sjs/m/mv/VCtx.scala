package io.suggest.sc.sjs.m.mv

import io.suggest.sc.sjs.m.mv.ctx.layout.LayoutCache
import io.suggest.sc.sjs.m.mv.ctx.grid.GridCtx
import io.suggest.sc.sjs.m.mv.ctx.nav.NavCtx
import org.scalajs.dom

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 11:22
 * Description: view context используется для фоновой передачи каких-то common-данных из контроллеров
 * в представления.
 * Например, кеширование найденных тегов через lazy val, используемых в нескольких местах.
 */
trait IVCtx extends GridCtx with LayoutCache with NavCtx {

  // Быстрый доступ к довольно частым полям DOM.
  def w = dom.window
  def d = dom.document

}

/** Дефолтовая реализация [[IVCtx]]. */
class VCtx extends IVCtx
