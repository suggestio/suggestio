package io.suggest.sc.m.boot

import io.suggest.sc.sc3.Sc3Pages
import japgolly.scalajs.react.extra.router.{Router, RouterCtl}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.12.2019 21:25
  * Description: Модель данных состояния SPA-роутера.
  */
case class MSpaRouterState(
                            router           : Router[Sc3Pages],
                            routerCtl        : RouterCtl[Sc3Pages],
                            canonicalRoute   : Option[Sc3Pages.MainScreen],
                          )
