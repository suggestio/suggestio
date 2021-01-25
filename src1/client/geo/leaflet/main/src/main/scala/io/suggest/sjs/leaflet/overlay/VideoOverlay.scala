package io.suggest.sjs.leaflet.overlay

import io.suggest.sjs.leaflet.map.{LatLngBounds, Layer}
import io.suggest.sjs.leaflet.{LEAFLET_IMPORT, LatLngBoundsExpression}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 17:53
  * @see [[https://leafletjs.com/reference-1.6.0.html#videooverlay]]
  */
@js.native
@JSImport(LEAFLET_IMPORT, "VideoOverlay")
class VideoOverlay(
                    video         : String | js.Array[String] | dom.html.Video,
                    bounds        : LatLngBoundsExpression,
                    val options   : VideoOverlayOptions = js.native,
                  )
  extends Layer
{

  def setOpacity(opacity: Double): this.type = js.native
  def bringToFront(): this.type = js.native
  def bringToBack(): this.type = js.native
  def setUrl(url: String): this.type = js.native
  def setBounds(bounds: LatLngBounds): this.type = js.native
  def getBounds(): LatLngBounds = js.native
  def getElement(): js.UndefOr[dom.html.Video] = js.native

}


trait VideoOverlayOptions extends ImageOverlayOptions {
  val autoplay: js.UndefOr[Boolean] = js.undefined
  val loop: js.UndefOr[Boolean] = js.undefined
  val keepAspectRatio: js.UndefOr[Boolean] = js.undefined
}