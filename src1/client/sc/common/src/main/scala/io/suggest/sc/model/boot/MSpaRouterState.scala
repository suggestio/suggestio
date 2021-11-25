package io.suggest.sc.model.boot

import io.suggest.spa.SioPages
import japgolly.scalajs.react.extra.router.{Router, RouterCtl}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.12.2019 21:25
  * Description: Модель данных состояния SPA-роутера.
  */
case class MSpaRouterState(
                            router           : Router[SioPages.Sc3],
                            routerCtl        : RouterCtl[SioPages.Sc3],
                            canonicalRoute   : Option[SioPages.Sc3],
                          )


object MSpaRouterState {

  implicit final class SpaRouterStateExt( private val spaRouterState: MSpaRouterState ) extends AnyVal {

    def isCanonicalRouteHasNodeId: Boolean =
      spaRouterState.canonicalRoute.exists(_.nodeId.nonEmpty)

  }

}
