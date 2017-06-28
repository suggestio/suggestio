package react.leaflet.circle

import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.path.circle.CircleOptions
import japgolly.scalajs.react.{JsComponent, Children}
import react.leaflet.event.MapComponentEventsProps

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:06
  * Description: react-leaflet API wrapper for Circle component.
  *
  * scala-js 0.6.14+ required.
  */

object CircleR {

  val component = JsComponent[CirclePropsR, Children.None, Null]( LCircleJs )

  def apply(props: CirclePropsR) = component( props )

}


@JSImport("react-leaflet", "Circle")
@js.native
object LCircleJs extends js.Object


@ScalaJSDefined
trait CirclePropsR extends MapComponentEventsProps with CircleOptions {

  /** Circle center. */
  val center  : LatLng

}
