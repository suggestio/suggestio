package io.suggest.sc.router

import io.suggest.routes.ScJsRoutes
import io.suggest.sc.ScConstants.JsRouter._
import org.scalajs.dom.Window

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 16:31
 * Description: Расширение DOM Window API для доступа к play js routes, т.е. к роутеру, которого может и не быть.
 */
@js.native
sealed trait WindowWithScRouterSafe extends js.Object {

  /**
   * Доступ к необязательному объекту-коду jsRoutes через window.
   * @return Роутер или undefined.
   */
  @JSName(NAME)
  def jsRoutes: UndefOr[ScJsRoutes.type] = js.native

  /**
   * Доступ к функции иниализации при асинхронной подгрузке js-роутера.
   * js-код роутера вызовет эту функцию, как только будет загружен в память.
   * Функция может быть перевыставлена несколько раз в случае активной долбежки роутера, это нормально.
   * @return function() или undefined.
   */
  @JSName(ASYNC_INIT_FNAME)
  var sioScJsRoutesAsyncInit: UndefOr[js.Function0[_]] = js.native

}


object WindowWithScRouterSafe {

  /** Приведение окна к вышеуказанному API. */
  implicit def wnd2routerWnd(wnd: Window): WindowWithScRouterSafe = {
    wnd.asInstanceOf[WindowWithScRouterSafe]
  }

}
