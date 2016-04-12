package io.suggest.sjs.mapbox.gl.camera

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.{Bearing_t, Zoom_t, Pitch_t}
import io.suggest.sjs.mapbox.gl.ll.LngLat

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:30
  * Description: Camera options API.
  */

object CameraOptions extends FromDict {
  override type T = CameraOptions
}

@js.native
trait CameraOptions extends js.Object {

  var center    : LngLat      = js.native

  var zoom      : Zoom_t      = js.native

  var bearing   : Pitch_t   = js.native

  var pitch     : Double      = js.native

  var around    : LngLat      = js.native

}

