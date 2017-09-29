package io.suggest.sjs.mapbox.gl.layer.circle

import io.suggest.sjs.mapbox.gl.Color_t
import io.suggest.sjs.mapbox.gl.layer.PaintProps

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 18:55
  * Description: API for painting properties for the circle layer.
  */

trait CirclePaintProps extends PaintProps {

  val `circle-radius`          : UndefOr[Int]            = js.undefined

  val `circle-color`           : UndefOr[Color_t]        = js.undefined

  val `circle-blur`            : UndefOr[Double]         = js.undefined

  val `circle-opacity`         : UndefOr[Double]         = js.undefined

  val `circle-translate`       : UndefOr[js.Array[Int]]  = js.undefined

  /** @see [[CircleTranslateAnchors]]. */
  val `circle-translate-anchor` : UndefOr[String]         = js.undefined

}
