package io.suggest.sjs.mapbox.gl.layer.circle

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.Color_t
import io.suggest.sjs.mapbox.gl.layer.PaintProps

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 18:55
  * Description: API for painting properties for the circle layer.
  */

object CirclePaintProps extends FromDict {

  override type T = CirclePaintProps

}


@js.native
trait CirclePaintProps extends PaintProps {

  @JSName("circle-radius")
  var circleRadius          : UndefOr[Int]            = js.native

  @JSName("circle-color")
  var circleColor           : UndefOr[Color_t]        = js.native

  @JSName("circle-blur")
  var circleBlur            : UndefOr[Double]         = js.native

  @JSName("circle-opacity")
  var circleOpacity         : UndefOr[Double]         = js.native

  @JSName("circle-translate")
  var circleTranslate       : UndefOr[js.Array[Int]]  = js.native

  @JSName("circle-translate-anchor")
  /** @see [[CircleTranslateAnchors]]. */
  var circleTranslateAnchor : UndefOr[String]         = js.native

}
