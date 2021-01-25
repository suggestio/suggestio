package io.suggest.sjs.leaflet.overlay

import io.suggest.sjs.leaflet.{LEAFLET_IMPORT, LatLngBoundsExpression}
import io.suggest.sjs.leaflet.layer.InteractiveLayerOptions
import io.suggest.sjs.leaflet.map.{LatLngBounds, Layer}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 17:38
  * @see [[https://leafletjs.com/reference-1.6.0.html#imageoverlay]]
  */

@js.native
@JSImport(LEAFLET_IMPORT, "ImageOverlay")
class ImageOverlay(
                    imageUrl      : String,
                    bounds        : LatLngBoundsExpression,
                    val options   : ImageOverlayOptions = js.native,
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
  def getElement(): js.UndefOr[dom.html.Image] = js.native

}


trait ImageOverlayOptions extends InteractiveLayerOptions {
  val opacity: js.UndefOr[Double] = js.undefined
  val alt: js.UndefOr[String] = js.undefined
  val crossOrigin: js.UndefOr[/*type CrossOrigin = */ Boolean | String] = js.undefined
  val errorOverlayUrl: js.UndefOr[String] = js.undefined
  val zIndex: js.UndefOr[Int] = js.undefined
  val className: js.UndefOr[String] = js.undefined
}
