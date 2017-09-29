package io.suggest.sjs.mapbox.gl.map

import io.suggest.sjs.mapbox.gl.Zoom_t
import io.suggest.sjs.mapbox.gl.anim.AnimationOptions
import io.suggest.sjs.mapbox.gl.camera.CameraOptions

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 13:43
  * Description: map.flyTo() options API.
  *
  * FlyOptions = CameraOptions | AnimationOptions + [real fly options]
  */

trait FlyOptions extends AnimationOptions with CameraOptions {

  val curve   : UndefOr[Double]   = js.undefined

  val minZoom : UndefOr[Zoom_t]   = js.undefined

  val speed   : UndefOr[Double]   = js.undefined

  val screenSpeed : UndefOr[Double] = js.undefined

}
