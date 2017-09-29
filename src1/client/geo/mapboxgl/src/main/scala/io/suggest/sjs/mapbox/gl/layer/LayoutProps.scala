package io.suggest.sjs.mapbox.gl.layer

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 18:59
  * Description: Common API for layout properties.
  */

trait LayoutProps extends js.Object {

  val visibility: UndefOr[String] = js.undefined

}
