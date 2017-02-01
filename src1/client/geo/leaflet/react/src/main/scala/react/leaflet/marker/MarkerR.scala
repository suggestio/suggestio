package react.leaflet.marker

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.MarkerOptions
import japgolly.scalajs.react.{JsComponentType, TopNode}
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 16:35
  * Description: React wrapper for react-leaflet Marker component.
  */

case class MarkerR(
  override val props: MarkerPropsR
)
  extends JsWrapperR[MarkerPropsR, HTMLElement] {

  override protected def _rawComponent = js.constructorOf[Marker]
}

@JSImport("react-leaflet", "Marker")
@js.native
sealed class Marker extends JsComponentType[MarkerPropsR, js.Object, TopNode]

@ScalaJSDefined
trait MarkerPropsR extends MarkerOptions {

  /** lat-lng coordinates for marker position. */
  val position      : LatLng

  val onClick       : UndefOr[js.Function1[Event, Unit]]         = js.undefined

  val onDragStart   : UndefOr[js.Function1[Event, Unit]]         = js.undefined

  val onMoveStart   : UndefOr[js.Function1[Event, Unit]]         = js.undefined

  val onDrag        : UndefOr[js.Function1[Event, Unit]]         = js.undefined

  val onDragEnd     : UndefOr[js.Function1[DragEndEvent, Unit]]  = js.undefined

  val onMoveEnd     : UndefOr[js.Function1[Event, Unit]]         = js.undefined

}

