package io.suggest.sjs.mapbox.gl.anim

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:31
  * Description: Animation options API.
  */

object AnimationOptions extends FromDict {
  override type T = AnimationOptions
}


@js.native
trait AnimationOptions extends js.Object {

  var duration: Double          = js.native

  var easing: js.Function1[Double, Double] = js.native

  var offset: js.Array[js.Any]  = js.native

  var animate: Boolean          = js.native

}
