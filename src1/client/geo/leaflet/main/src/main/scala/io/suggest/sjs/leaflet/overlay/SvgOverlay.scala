package io.suggest.sjs.leaflet.overlay

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.LatLngBoundsExpression
import io.suggest.sjs.leaflet.map.{LatLngBounds, Layer}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 18:03
  * @see [[https://leafletjs.com/reference-1.6.0.html#svgoverlay]]
  */
@js.native
@JSImport(LEAFLET_IMPORT, "SVGOverlay")
class SvgOverlay(
                  svgImage: String | dom.svg.Element,
                  bounds: LatLngBoundsExpression,
                  val options: ImageOverlayOptions = js.native,
                )
  extends Layer
{

  def setOpacity(opacity: Double): this.type = js.native
  def bringToFront(): this.type = js.native
  def bringToBack(): this.type = js.native
  def setUrl(url: String): this.type = js.native
  def setBounds(bounds: LatLngBounds): this.type = js.native
  def setZIndex(value: Int): this.type = js.native
  def getBounds(): LatLngBounds = js.native
  def getElement(): js.UndefOr[dom.svg.Element] = js.native

}
