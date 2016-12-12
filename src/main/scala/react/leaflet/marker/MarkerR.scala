package react.leaflet.marker

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.MarkerOptions
import io.suggest.sjs.leaflet.marker.icon.Icon
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 16:35
  * Description: React wrapper for react-leaflet Marker component.
  */

object MarkerR {

  def apply(
    position      : LatLng,
    icon          : UndefOr[Icon]     = js.undefined,
    clickable     : UndefOr[Boolean]  = js.undefined,
    draggable     : UndefOr[Boolean]  = js.undefined,
    keyboard      : UndefOr[Boolean]  = js.undefined,
    title         : UndefOr[String]   = js.undefined,
    alt           : UndefOr[String]   = js.undefined,
    zIndexOffset  : UndefOr[Int]      = js.undefined,
    opacity       : UndefOr[Int]      = js.undefined,
    riseOnHover   : UndefOr[Boolean]  = js.undefined,
    riseOffset    : UndefOr[Int]      = js.undefined
  ): MarkerR = {

    val p = js.Dynamic.literal().asInstanceOf[MarkerPropsR]

    p.position = position
    icon.foreach( p.icon = _ )
    clickable.foreach( p.clickable = _ )
    draggable.foreach( p.draggable = _ )
    keyboard.foreach( p.draggable = _ )
    title.foreach( p.title = _ )
    alt.foreach( p.alt = _ )
    zIndexOffset.foreach( p.zIndexOffset = _ )
    opacity.foreach( p.opacity = _ )
    riseOnHover.foreach( p.riseOnHover = _ )
    riseOffset.foreach( p.riseOffset = _ )

    MarkerR(p)
  }

}


case class MarkerR(
  override val props: MarkerPropsR
)
  extends JsWrapperR[MarkerPropsR, HTMLElement] {

  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Marker

}


@js.native
trait MarkerPropsR extends MarkerOptions {

  var position      : LatLng

}

