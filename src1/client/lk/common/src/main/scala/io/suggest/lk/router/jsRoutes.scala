package io.suggest.lk.router

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import io.suggest.js.JsRoutesConst.GLOBAL_NAME
import io.suggest.routes.StaticRoutesController

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 17:43
 * Description: Global-scope интерфейс для js-роутеров.
 */
@js.native
@JSGlobal(GLOBAL_NAME)
object jsRoutes extends js.Object {

  def controllers: Controllers = js.native

}


@js.native
sealed trait Controllers extends StaticRoutesController {

  def LkBill2: LkBill2Routes = js.native

  def MarketAd: MarketAdFormCtl = js.native

}
