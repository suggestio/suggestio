package io.suggest.sjs.mapbox.gl.map

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.Zoom_t
import io.suggest.sjs.mapbox.gl.anim.AnimationOptions
import io.suggest.sjs.mapbox.gl.camera.CameraOptions

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 13:43
  * Description: map.flyTo() options API.
  *
  * FlyOptions = CameraOptions | AnimationOptions + [real fly options]
  */
object FlyOptions extends FromDict {
  override type T = FlyOptions
}


@js.native
class FlyOptions extends AnimationOptions with CameraOptions {

  var curve: Double = js.native

  var minZoom: Zoom_t = js.native

  var speed  : Double = js.native

  var screenSpeed: Double = js.native

}
