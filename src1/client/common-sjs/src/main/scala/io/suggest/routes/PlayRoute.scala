package io.suggest.routes

import japgolly.univeq.UnivEq

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 15:28
 * Description: Интерфейс одной play-роуты. Возвращается любым экшеном js-роутера.
 */

@js.native
sealed trait PlayRoute extends js.Object {

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

object PlayRoute {

  @inline implicit def univEq: UnivEq[PlayRoute] = UnivEq.force


  /** Поддержка экстракции роут для инстансов [[PlayRoute]]. */
  implicit object PlayRouteExtractor extends HttpRouteExtractor[PlayRoute] {
    override def url(t: PlayRoute)    = t.url
    override def method(t: PlayRoute) = t.method

    override def absoluteUrl(t: PlayRoute, secure: Boolean): String =
      t.absoluteURL(secure)

    override def webSocketUrl(t: PlayRoute, secure: Boolean): String =
      t.webSocketURL(secure)
  }

}

