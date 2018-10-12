package io.suggest.sjs.common.model

import japgolly.univeq.UnivEq

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 15:28
 * Description: Интерфейс одной роуты. Возвращается любым экшеном js-роутера.
 */

@js.native
sealed trait Route extends js.Object {

  /** Используемый HTTP-метод. GET, POST и т.д. */
  val method: String = js.native

  /** relative URL запроса. */
  val url: String = js.native

  /** Абсолютный URL запроса по мнению сервера.
    * @param secure true -- использовать https вместо http. [false]
    * @return Строку абсолютной ссылки на текущий ресурс.
    */
  def absoluteURL(secure: Boolean = js.native): String = js.native

  /**
   * Абсолютный websocket URL по мнению сервера.
   * @param secure true -- использовать wss вместо ws. [false]
   * @return Строку абсолютной ссылки на вебсокет текущего ресурса.
   */
  def webSocketURL(secure: Boolean = js.native): String = js.native

}

object Route {
  implicit def univEq: UnivEq[Route] = UnivEq.force
}

