package io.suggest.lk.adv.geo.u

import io.suggest.lk.adv.geo.LkAdvGeoFormCircuit
import io.suggest.lk.adv.geo.r.AdvGeoFormR
import japgolly.scalajs.react.extra.router._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 13:33
  * Description: scalajs-react router.
  */
object RouterConfig {

  sealed trait Loc

  case object FormLoc extends Loc

  val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>

    import dsl._

    // Form need some activity.
    val formWrapper = LkAdvGeoFormCircuit.connect(m => m)

    staticRoute(root, FormLoc) ~> renderR { ctl =>
      ???   //TODO formWrapper( AdvGeoFormR(_) )
    }

    ???
  }

}
