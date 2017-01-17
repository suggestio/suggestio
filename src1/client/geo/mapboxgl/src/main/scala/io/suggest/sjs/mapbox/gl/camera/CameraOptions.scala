package io.suggest.sjs.mapbox.gl.camera

import io.suggest.sjs.mapbox.gl.{Pitch_t, Zoom_t}
import io.suggest.sjs.mapbox.gl.ll.LngLat

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:30
  * Description: Camera options API.
  */

@ScalaJSDefined
trait CameraOptions extends js.Object {

  val center    : UndefOr[LngLat]      = js.undefined

  val zoom      : UndefOr[Zoom_t]      = js.undefined

  val bearing   : UndefOr[Pitch_t]     = js.undefined

  val pitch     : UndefOr[Double]      = js.undefined

  val around    : UndefOr[LngLat]      = js.undefined

}

