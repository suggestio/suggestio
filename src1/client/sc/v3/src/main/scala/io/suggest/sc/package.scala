package io.suggest

import japgolly.scalajs.react.extra.router.RouterCtl
import io.suggest.sc.root.m.Sc3Pages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.17 17:33
  */
package object sc {

  type GetRouterCtlF = () => RouterCtl[Sc3Pages]

}
