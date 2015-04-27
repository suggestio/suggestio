package io.suggest.sjs.common.model

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 15:28
 * Description: Интерфейс одной роуты. Возвращается любым экшеном js-роутера.
 */

class Route extends js.Object {
  // def ajax() -- нельзя, т.к. deprecated и будет удалена, и зависит от jQuery.

  /** Используемый HTTP-метод. GET, POST и т.д. */
  def method: String = js.native

  /** relative URL запроса. */
  def url: String = js.native

  /** Абсолютный URL запроса по мнению сервера. */
  def absoluteURL(): String = js.native

  /** Абсолютный websocket URL по мнению сервера. */
  def webSocketURL(): String = js.native
}
