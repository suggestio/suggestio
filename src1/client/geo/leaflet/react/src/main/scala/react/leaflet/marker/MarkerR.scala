package react.leaflet.marker

import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.MarkerOptions
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 16:35
  * Description: React wrapper for react-leaflet Marker component.
  */

object MarkerR {

  val component = JsComponent[MarkerPropsR, Children.None, Null]( LMarkerJs )

  def apply(props: MarkerPropsR) = component(props)

}


@JSImport("react-leaflet", "Marker")
@js.native
object LMarkerJs extends js.Object // JsComponentType[MarkerPropsR, js.Object, TopNode]


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

