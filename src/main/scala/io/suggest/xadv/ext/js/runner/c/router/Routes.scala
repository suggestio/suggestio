package io.suggest.xadv.ext.js.runner.c.router

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.03.15 11:16
 * Description: Роуты, доступные из jsRoutes описываются здесь.
 */

/** Интерфейс routes.controllers. */
class Controllers extends js.Object {
  def Static: CtlStatic = js.native
}


/** Интерфейс контроллера Static. */
class CtlStatic extends js.Object {
  def popupCheckContent(): Route = js.native
}


/** Интерфейс одной роуты. Возвращается любым экшеном js-роутера. */
class Route extends js.Object {
  // def ajax() -- нельзя, т.к. deprecated и будет удалена, и зависит от jQuery.
  /** Используемый HTTP-метод. GET, POST и т.д. */
  def method: String = js.native

  /** relative URL запроса. */
  def url: String = js.native

  /** Абсолютный URL запроса по мнению сервера. */
  def absoluteURL: String = js.native

  /** Абсолютный websocket URL по мнению сервера. */
  def webSocketURL: String = js.native
}
