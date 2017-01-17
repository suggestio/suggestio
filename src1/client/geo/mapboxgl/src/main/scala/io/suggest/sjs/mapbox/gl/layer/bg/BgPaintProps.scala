package io.suggest.sjs.mapbox.gl.layer.bg

import io.suggest.sjs.mapbox.gl.Color_t
import io.suggest.sjs.mapbox.gl.layer.PaintProps

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSName, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 18:56
  * Description: Common API for layer paint properties.
  */

@ScalaJSDefined
trait BgPaintProps extends PaintProps {

  @JSName("background-color")
  val backgroundColor     : UndefOr[Color_t]  = js.undefined

  @JSName("background-pattern")
  val backgroundPattern   : UndefOr[String]   = js.undefined

  @JSName("background-opacity")
  val backgroundOpacity   : UndefOr[Double]   = js.undefined

}
