package react.leaflet.poly

import io.suggest.sjs.leaflet.PolygonLatLngs_t
import io.suggest.sjs.leaflet.path.poly.PolylineOptions
import japgolly.scalajs.react.{JsComponent, Children}
import react.leaflet.event.MapComponentEventsProps

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 22:19
  * Description: React-component for L.Polygon rendering.
  */
object PolygonR {

  val component = JsComponent[PolygonPropsR, Children.None, Null]( LPolygonJsR )

  def apply(props: PolygonPropsR) = component(props)

}


/** API Facade for react-leaflet Polygon.js.  */
@JSImport("react-leaflet", "Polygon")
@js.native
object LPolygonJsR extends js.Object  // JsComponentType[PolygonPropsR, js.Object, TopNode]


/** Polyline props & options. */
@ScalaJSDefined
trait PolygonPropsR
  extends MapComponentEventsProps
  with PolylineOptions
{

  val positions: PolygonLatLngs_t

}
