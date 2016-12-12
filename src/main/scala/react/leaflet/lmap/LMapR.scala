package react.leaflet.lmap

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.event.LocationEvent
import io.suggest.sjs.leaflet.map.{LatLng, LatLngBounds, MapOptions, Zoom_t}
import org.scalajs.dom.raw.HTMLDivElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 12:20
  * Description: React leaflet map wrapper по упрощенным технологиям.
  * @see [[https://github.com/chandu0101/scalajs-react-components/blob/master/doc/InteropWithThirdParty.md]]
  */

object LMapR {

  def apply(
    // Error: Set map center and zoom first.
    center    : LatLng,
    zoom      : Zoom_t,
    animate   : UndefOr[Boolean]       = js.undefined,
    bounds    : UndefOr[LatLngBounds]  = js.undefined,
    minZoom   : UndefOr[Zoom_t]        = js.undefined,
    maxZoom   : UndefOr[Zoom_t]        = js.undefined,
    className : UndefOr[String]        = js.undefined,
    style     : UndefOr[String]        = js.undefined,
    id        : UndefOr[String]        = js.undefined,
    onLocationFound: UndefOr[LocationEvent => _] = js.undefined,
    useFlyTo  : UndefOr[Boolean]       = js.undefined
  ): LMapR = {
    val p = js.Dynamic.literal().asInstanceOf[LMapPropsR]

    p.center = center
    p.zoom = zoom
    animate.foreach( p.animate = _ )
    bounds.foreach( p.bounds = _ )
    minZoom.foreach( p.minZoom = _ )
    maxZoom.foreach( p.maxZoom = _ )
    className.foreach( p.className = _ )
    style.foreach( p.style = _ )
    id.foreach( p.id = _ )
    onLocationFound.foreach(p.onlocationfound = _)
    useFlyTo.foreach( p.useFlyTo = _ )

    LMapR( p )
  }

}


case class LMapR(
  override val props: LMapPropsR
)
  extends JsWrapperR[LMapPropsR, HTMLDivElement]
{

  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Map

}


@js.native
trait LMapPropsR extends MapOptions {

  var animate         : Boolean        = js.native
  var bounds          : LatLngBounds   = js.native
  var boundsUndefOrs  : js.Object      = js.native
  var className       : String         = js.native
  var style           : String         = js.native
  var id              : String         = js.native
  var useFlyTo        : Boolean        = js.native

  /**
    * Optional reaction about detected location (L.control.locate).
    * Handled automatically inside MapComponent.bindLeafletEvents().
    */
  var onlocationfound: js.Function1[LocationEvent,_] = js.native

}
