package io.suggest.sjs.mapbox.gl.layer

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 18:59
  * Description: Common API for layout properties.
  */
object LayoutProps extends FromDict {
  override type T = LayoutProps
}


@js.native
trait LayoutProps extends js.Object {

  var visibility: UndefOr[String] = js.native

}
