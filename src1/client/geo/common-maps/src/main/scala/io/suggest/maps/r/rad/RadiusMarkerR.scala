package io.suggest.maps.r.rad

import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.maps.u.{MapIcons, MapsUtil}
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.marker.{MarkerPropsR, MarkerR}
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.maps.m.{RadiusDragEnd, RadiusDragStart, RadiusDragging}
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}

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

    // Стабильные инстансы callback-функций для маркера радиуса.
    private val _radiusDragStartF = cbFun1ToJsCb { _: Event => _dispatch(RadiusDragStart) }
    private val _radiusDraggingF  = cbFun1ToJsCb( _markerDragging(_: Event, RadiusDragging) )
    private val _radiusDragEndF   = cbFun1ToJsCb( _markerDragEnd(_: DragEndEvent, RadiusDragEnd) )


    def render(geoPointOptProxy: Props): ReactElement = {
      for {
        geoPoint <- geoPointOptProxy()
      } yield {
        MarkerR(
          new MarkerPropsR {
            override val position    = MapsUtil.geoPoint2LatLng(geoPoint)
            override val draggable   = true
            override val icon        = _radiusIcon

            // Привязка событий:
            override val onDragStart = _radiusDragStartF
            override val onDrag      = _radiusDraggingF
            override val onDragEnd   = _radiusDragEndF
          }
        )()
      }
    }

  }


  val component = ReactComponentB[Props]("RadiusMarker")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(geoPointOptProxy: Props) = component(geoPointOptProxy)

}
