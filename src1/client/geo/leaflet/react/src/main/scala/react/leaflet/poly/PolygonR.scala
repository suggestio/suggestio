package react.leaflet.poly

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.PolygonLatLngs_t
import io.suggest.sjs.leaflet.path.poly.PolylineOptions
import japgolly.scalajs.react.{JsComponentType, TopNode}
import org.scalajs.dom.Element
import react.leaflet.event.MapComponentEventsProps

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 22:19
  * Description: React-component for L.Polygon rendering.
  */
case class PolygonR( override val props: PolygonPropsR )
  extends JsWrapperR[PolygonPropsR, Element]
{
  override protected def _rawComponent: js.Dynamic = js.constructorOf[Polygon]
}


/** API Facade for react-leaflet Polygon.js.  */
@JSImport("react-leaflet", "Polygon")
@js.native
sealed class Polygon extends JsComponentType[PolygonPropsR, js.Object, TopNode]


/** Polyline props & options. */
@ScalaJSDefined
trait PolygonPropsR
  extends MapComponentEventsProps
  with PolylineOptions
{

  val positions: PolygonLatLngs_t

}
