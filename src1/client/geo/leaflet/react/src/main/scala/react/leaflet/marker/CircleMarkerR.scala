package react.leaflet.marker

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.path.circle.CircleMarkerOptions
import japgolly.scalajs.react.{JsComponentType, TopNode}
import react.leaflet.event.MapComponentEventsProps

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 15:02
  * Description: sjs-react wrapper for react-leaflet's CircleMarker.
  */
case class CircleMarkerR( props: CircleMarkerPropsR )
  extends JsWrapperR[CircleMarkerPropsR, TopNode]
{
  override protected def _rawComponent = js.constructorOf[CircleMarker]
}

@JSImport("react-leaflet", "CircleMarker")
@js.native
sealed class CircleMarker extends JsComponentType[CircleMarkerPropsR, js.Object, TopNode]


/** Circle-marker rendering args. */
@ScalaJSDefined
trait CircleMarkerPropsR extends CircleMarkerOptions with MapComponentEventsProps {

  /** Circle marker center coords. */
  val center: LatLng

}