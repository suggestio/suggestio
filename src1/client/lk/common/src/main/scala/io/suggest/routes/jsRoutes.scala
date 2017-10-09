package io.suggest.routes

import scala.language.implicitConversions
import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 17:43
 * Description: Global-scope интерфейс для js-роутеров.
 */
object JsRoutes_LkControllers {

  implicit def toLkControllers(controllers: Controllers): JsRoutes_LkControllers = {
    controllers.asInstanceOf[JsRoutes_LkControllers]
  }

}


@js.native
sealed trait JsRoutes_LkControllers extends js.Object {

  val LkBill2: LkBill2Routes = js.native

  val MarketAd: MarketAdFormCtl = js.native

}
