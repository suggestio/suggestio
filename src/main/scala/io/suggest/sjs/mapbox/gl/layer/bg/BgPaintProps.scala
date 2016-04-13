package io.suggest.sjs.mapbox.gl.layer.bg

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.Color_t
import io.suggest.sjs.mapbox.gl.layer.PaintProps

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 18:56
  * Description: Common API for layer paint properties.
  */
object BgPaintProps extends FromDict {
  override type T = BgPaintProps
}

@js.native
trait BgPaintProps extends PaintProps {

  @JSName("background-color")
  var backgroundColor     : UndefOr[Color_t]  = js.native

  @JSName("background-pattern")
  var backgroundPattern   : UndefOr[String]   = js.native

  @JSName("background-opacity")
  var backgroundOpacity   : UndefOr[Double]   = js.native

}
