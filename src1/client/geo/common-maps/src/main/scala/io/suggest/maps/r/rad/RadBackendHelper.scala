package io.suggest.maps.r.rad

import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.maps.m.IMapsAction
import io.suggest.maps.u.MapsUtil
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.marker.Marker
import japgolly.scalajs.react.{BackendScope, Callback}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 17:16
  * Description: Внутренняя утиль для react-компонентов Rad.
  */
class RadBackendHelper[Props, State]($: BackendScope[ModelProxy[Props], State]) {

  /** Событие начала перетаскивания маркера. */
  protected def _dispatch(msg: IMapsAction): Callback = {
    dispatchOnProxyScopeCB($, msg)
  }

  /** События таскания какого-то маркера. */
  protected def _markerDragging(e: Event, msg: MGeoPoint => IMapsAction): Callback = {
    val latLng = e.target
      .asInstanceOf[Marker]
      .getLatLng()
    val mgp = MapsUtil.latLng2geoPoint( latLng )
    dispatchOnProxyScopeCB($, msg(mgp))
  }

  /** Событие завершения перетаскивания маркера. */
  protected def _markerDragEnd(e: DragEndEvent, msg: MGeoPoint => IMapsAction): Callback = {
    _markerDragging(e, msg)
  }

}

