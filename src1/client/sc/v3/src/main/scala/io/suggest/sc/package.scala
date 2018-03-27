package io.suggest

import io.suggest.sc.sc3.Sc3Pages
import japgolly.scalajs.react.extra.router.RouterCtl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.17 17:33
  */
package object sc {

  type GetRouterCtlF = () => RouterCtl[Sc3Pages]

}
