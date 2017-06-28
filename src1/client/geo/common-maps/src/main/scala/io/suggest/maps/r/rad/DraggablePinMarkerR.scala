package io.suggest.maps.r.rad

import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m._
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.react.ReactCommonUtil.Implicits.vdomElOptionExt
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import react.leaflet.marker.{MarkerPropsR, MarkerR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 17:13
  * Description: Компонент таскабельного центрального маркера.
  */
object DraggablePinMarkerR {

  type Props = ModelProxy[Option[MGeoPoint]]


  class Backend($: BackendScope[Props, Unit]) extends RadBackendHelper($) {

    private val _pinIcon = MapIcons.pinMarkerIcon()

    // Функции-коллбеки для маркера центра круга.
    private val _centerClickF     = cbFun1ToJsCb { _: Event => _dispatch( RadCenterClick ) }
    private val _centerDragStartF = cbFun1ToJsCb { _: Event => _dispatch(RadCenterDragStart) }
    private val _centerDraggingF  = cbFun1ToJsCb( _markerDragging(_: Event, RadCenterDragging) )
    private val _centerDragEndF   = cbFun1ToJsCb( _markerDragEnd(_: DragEndEvent, RadCenterDragEnd) )

    def render(latLngProxy: Props): VdomElement = {
      latLngProxy().whenDefinedEl { geoPoint =>
        MarkerR(
          new MarkerPropsR {
            // Параметры рендера:
            override val position    = MapsUtil.geoPoint2LatLng( geoPoint )
            override val draggable   = true
            override val clickable   = true
            override val icon        = _pinIcon

            // Привязка событий:
            override val onClick     = _centerClickF
            override val onDragStart = _centerDragStartF
            override val onDrag      = _centerDraggingF
            override val onDragEnd   = _centerDragEndF
          }
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("DragPinMark")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(latLngProxy: Props) = component(latLngProxy)

}
