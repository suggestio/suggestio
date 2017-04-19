package io.suggest.maps.r.rad

import diode.react.ModelProxy
import io.suggest.maps.m._
import io.suggest.maps.u.MapIcons
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.react.ReactCommonUtil.cbFun1TojsCallback
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.map.LatLng
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.marker.{MarkerPropsR, MarkerR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 17:13
  * Description: Компонент таскабельного центрального маркера.
  */
object DraggablePinMarkerR {

  type Props = ModelProxy[Option[LatLng]]


  class Backend($: BackendScope[Props, Unit]) extends RadBackendHelper($) {

    private val _pinIcon = MapIcons.pinMarkerIcon()

    // Функции-коллбеки для маркера центра круга.
    private val _centerClickF     = cbFun1TojsCallback { _: Event => _dispatch( RadCenterClick ) }
    private val _centerDragStartF = cbFun1TojsCallback { _: Event => _dispatch(RadCenterDragStart) }
    private val _centerDraggingF  = cbFun1TojsCallback( _markerDragging(_: Event, RadCenterDragging) )
    private val _centerDragEndF   = cbFun1TojsCallback( _markerDragEnd(_: DragEndEvent, RadCenterDragEnd) )

    def render(latLngProxy: Props): ReactElement = {
      for {
        latLng <- latLngProxy()
      } yield {
        MarkerR(
          new MarkerPropsR {
            // Параметры рендера:
            override val position    = latLng
            override val draggable   = true
            override val clickable   = true
            override val icon        = _pinIcon

            // Привязка событий:
            override val onClick     = _centerClickF
            override val onDragStart = _centerDragStartF
            override val onDrag      = _centerDraggingF
            override val onDragEnd   = _centerDragEndF
          }
        )()
      }
    }

  }


  val component = ReactComponentB[Props]("DragPinMark")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(latLngProxy: Props) = component(latLngProxy)

}
