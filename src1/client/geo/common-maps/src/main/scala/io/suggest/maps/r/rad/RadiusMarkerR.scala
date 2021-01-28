package io.suggest.maps.r.rad

import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.maps.u.{MapIcons, MapsUtil}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.maps.m.{RadiusDragEnd, RadiusDragStart, RadiusDragging}
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event, LeafletEventHandlerFnMap}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import org.js.react.leaflet.{Marker, MarkerProps}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 11:19
  * Description: React-компонент маркера радиуса в составе RadR.
  */
object RadiusMarkerR {

  type Props = ModelProxy[Option[MGeoPoint]]


  class Backend($: BackendScope[Props, Unit]) extends RadBackendHelper($) {

    private val _radiusIcon = MapIcons.radiusMarkerIcon()

    /** инстансы callback-функций для маркера радиуса. */
    private val _eventHandlers = new LeafletEventHandlerFnMap {
      override val dragstart = cbFun1ToJsCb { _: Event => _dispatch(RadiusDragStart) }
      override val drag = cbFun1ToJsCb( _markerDragging(_: Event, RadiusDragging) )
      override val dragend = cbFun1ToJsCb( _markerDragEnd(_: DragEndEvent, RadiusDragEnd) )
    }


    def render(geoPointOptProxy: Props): VdomElement = {
      geoPointOptProxy().whenDefinedEl { geoPoint =>
        Marker.component(
          new MarkerProps {
            override val position    = MapsUtil.geoPoint2LatLng(geoPoint)
            override val draggable   = true
            override val icon        = _radiusIcon
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
