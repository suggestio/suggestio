package react.leaflet.lmap

import io.suggest.sjs.leaflet.map.{LatLng, LatLngBounds, Zoom_t}
import org.scalajs.dom.raw.HTMLDivElement
import react.leaflet.WrapperR

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
    useFlyTo.foreach( p.useFlyTo = _ )

    LMapR( p )
  }

}


case class LMapR(
  override val props: LMapPropsR
)
  extends WrapperR[LMapPropsR, HTMLDivElement]
{

  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Map

}


@js.native
trait LMapPropsR extends js.Object {

  var center          : UndefOr[LatLng]         = js.native
  var zoom            : UndefOr[Zoom_t]         = js.native
  var animate         : UndefOr[Boolean]        = js.native
  var bounds          : UndefOr[LatLngBounds]   = js.native
  var boundsUndefOrs  : UndefOr[js.Object]      = js.native
  var maxZoom         : UndefOr[Zoom_t]         = js.native
  var minZoom         : UndefOr[Zoom_t]         = js.native
  var className       : UndefOr[String]         = js.native
  var style           : UndefOr[String]         = js.native
  var id              : UndefOr[String]         = js.native
  var useFlyTo        : UndefOr[Boolean]        = js.native

}
