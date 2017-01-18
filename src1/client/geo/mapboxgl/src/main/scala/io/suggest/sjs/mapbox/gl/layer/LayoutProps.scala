package io.suggest.sjs.mapbox.gl.layer

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 18:59
  * Description: Common API for layout properties.
  */

@ScalaJSDefined
trait LayoutProps extends js.Object {

  val visibility: UndefOr[String] = js.undefined

}
