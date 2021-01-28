package io.suggest.maps.r.rad

import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m._
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event, LeafletEventHandlerFnMap}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import org.js.react.leaflet.{Marker, MarkerProps}

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

    /** Функции-коллбеки для маркера центра круга. */
    private val _eventHandlers = new LeafletEventHandlerFnMap {
      override val click = cbFun1ToJsCb { _: Event => _dispatch( RadCenterClick ) }
      override val dragstart = cbFun1ToJsCb { _: Event => _dispatch(RadCenterDragStart) }
      override val drag = cbFun1ToJsCb( _markerDragging(_: Event, RadCenterDragging) )
      override val dragend = cbFun1ToJsCb( _markerDragEnd(_: DragEndEvent, RadCenterDragEnd) )
    }

    def render(latLngProxy: Props): VdomElement = {
      latLngProxy().whenDefinedEl { geoPoint =>
        Marker.component(
          new MarkerProps {
            // Параметры рендера:
            override val position    = MapsUtil.geoPoint2LatLng( geoPoint )
            override val draggable   = true
            override val clickable   = true
            override val icon        = _pinIcon
            override val eventHandlers = _eventHandlers
          }
        )()
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
