package io.suggest.sc.view.jsrouter

import io.suggest.routes.{JsRoutesConst, routes}
import io.suggest.sc.ScConstants
import org.scalajs.dom.Window

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSExportTopLevel, JSGlobalScope, JSName}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 16:31
 * Description: Расширение DOM Window API для доступа к play js routes, т.е. к роутеру, которого может и не быть.
 */
@js.native
@JSGlobalScope
object GlobalScRouterSafe extends js.Object {

  /**
   * Доступ к необязательному объекту-коду jsRoutes через window.
   * @return Роутер или undefined.
   */
  @JSName( JsRoutesConst.GLOBAL_NAME )
  var jsRoutes: js.UndefOr[routes.type] = js.native

}
