package io.suggest.sjs.mapbox.gl.control.nav

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 15:57
  * Description:
  */
object NavOptions extends FromDict {
  override type T = NavOptions
}

@js.native
trait NavOptions extends js.Object {

  /** See [[NavPositions]]. */
  var position: String = js.native

}
