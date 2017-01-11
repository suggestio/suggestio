package react.leaflet.marker

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.MarkerOptions
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

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

  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Marker

}


@ScalaJSDefined
trait MarkerPropsR extends MarkerOptions {

  /** lat-lng coordinates for marker position. */
  val position      : LatLng

  val onDragStart   : UndefOr[js.Function1[Event, Unit]]         = js.undefined

  val onMoveStart   : UndefOr[js.Function1[Event, Unit]]         = js.undefined

  val onDrag        : UndefOr[js.Function1[Event, Unit]]         = js.undefined

  val onDragEnd     : UndefOr[js.Function1[DragEndEvent, Unit]]  = js.undefined

  val onMoveEnd     : UndefOr[js.Function1[Event, Unit]]         = js.undefined

}

