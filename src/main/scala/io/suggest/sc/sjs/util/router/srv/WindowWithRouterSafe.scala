package io.suggest.sc.sjs.util.router.srv

import io.suggest.sc.ScConstants.{JS_ROUTER_NAME, JS_ROUTER_ASYNC_INIT_FNAME}
import org.scalajs.dom.Window

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 16:31
 * Description: Расширение DOM Window API для доступа к play js routes, т.е. к роутеру, которого может и не быть.
 */
trait WindowWithRouterSafe extends Window {

  /**
   * Доступ к необязательному объекту-коду jsRoutes через window.
   * @return Роутер или undefined.
   */
  @JSName(JS_ROUTER_NAME)
  def jsRoutes: UndefOr[routes.type] = js.native

  /**
   * Доступ к функции иниализации при асинхронной подгрузке js-роутера.
   * js-код роутера вызовет эту функцию, как только будет загружен в память.
   * Функция может быть перевыставлена несколько раз в случае активной долбежки роутера, это нормально.
   * @return function() или undefined.
   */
  @JSName(JS_ROUTER_ASYNC_INIT_FNAME)
  var sioScJsRoutesAsyncInit: UndefOr[js.Function0[_]] = js.native

}


object WindowWithRouterSafe {

  /** Приведение окна к вышеуказанному API. */
  implicit def wnd2routerWnd(wnd: Window): WindowWithRouterSafe = {
    wnd.asInstanceOf[WindowWithRouterSafe]
  }

}
