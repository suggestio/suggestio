package io.suggest.ads.a

import io.suggest.routes.Controllers
import io.suggest.sjs.common.model.Route

import scala.scalajs.js
import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 22:36
  * Description: js-Роуты до LkAds.
  */

@js.native
sealed trait LkAdsRoutes extends js.Object {

  def LkAds: LkAdsCtlRoutes = js.native

}
object LkAdsRoutes {
  implicit def apply(ctls: Controllers): LkAdsRoutes = {
    ctls.asInstanceOf[LkAdsRoutes]
  }
}


@js.native
sealed trait LkAdsCtlRoutes extends js.Object {

  def getAds(rcvrKey: String, offset: Int): Route = js.native

}


