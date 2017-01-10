package react.leaflet.marker

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.path.circle.CircleMarkerOptions
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 15:02
  * Description: sjs-react wrapper for react-leaflet's CircleMarker.
  */
case class CircleMarkerR( props: CircleMarkerPropsR )
  extends JsWrapperR[CircleMarkerPropsR, HTMLElement]
{
  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.CircleMarker
}


/** Circle-marker rendering args. */
@ScalaJSDefined
trait CircleMarkerPropsR extends CircleMarkerOptions {

  /** Circle marker center coords. */
  val center: LatLng

}