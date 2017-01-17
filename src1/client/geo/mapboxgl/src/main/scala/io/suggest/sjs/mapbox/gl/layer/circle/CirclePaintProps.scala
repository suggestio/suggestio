package io.suggest.sjs.mapbox.gl.layer.circle

import io.suggest.sjs.mapbox.gl.Color_t
import io.suggest.sjs.mapbox.gl.layer.PaintProps

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSName, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 18:55
  * Description: API for painting properties for the circle layer.
  */

@ScalaJSDefined
trait CirclePaintProps extends PaintProps {

  @JSName("circle-radius")
  val circleRadius          : UndefOr[Int]            = js.undefined

  @JSName("circle-color")
  val circleColor           : UndefOr[Color_t]        = js.undefined

  @JSName("circle-blur")
  val circleBlur            : UndefOr[Double]         = js.undefined

  @JSName("circle-opacity")
  val circleOpacity         : UndefOr[Double]         = js.undefined

  @JSName("circle-translate")
  val circleTranslate       : UndefOr[js.Array[Int]]  = js.undefined

  @JSName("circle-translate-anchor")
  /** @see [[CircleTranslateAnchors]]. */
  val circleTranslateAnchor : UndefOr[String]         = js.undefined

}
