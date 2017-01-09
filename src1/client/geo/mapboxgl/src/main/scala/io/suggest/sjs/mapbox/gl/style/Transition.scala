package io.suggest.sjs.mapbox.gl.style

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 14:14
  * Description: Style.transition model API.
  */

object Transition extends FromDict {
  override type T = Transition
}


@js.native
trait Transition extends js.Object {

  var duration: Double = js.native

  var delay: Double = js.native

}
