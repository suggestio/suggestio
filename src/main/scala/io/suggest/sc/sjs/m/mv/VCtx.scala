package io.suggest.sc.sjs.m.mv

import io.suggest.sc.sjs.m.mv.ctx.layout.LayoutCache
import io.suggest.sc.sjs.m.mv.ctx.tile.TileCache
import org.scalajs.dom

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 11:22
 * Description: view context используется для фоновой передачи каких-то common-данных из контроллеров
 * в представления.
 * Например, кеширование найденных тегов через lazy val, используемых в нескольких местах.
 */
trait IVCtx extends TileCache with LayoutCache {

  // Быстрый доступ к довольно частым полям DOM.
  def w = dom.window
  def d = dom.document

}

class VCtx extends IVCtx
