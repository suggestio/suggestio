package io.suggest.maps.vm.inp

import io.suggest.sjs.common.vm.attr.DoubleInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.{Leaflet => L}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.16 16:10
  * Description: Утиль для пакетной работы с vm'ками.
  */
trait InputsHelpers {

  protected[this] def _find[T1](m: FindElT { type T = T1 }) = m.find()

  trait SetLatLon {

    def lat: Option[DoubleInputValueT]
    def lon: Option[DoubleInputValueT]

    /** Апдейт значений в полях lat и lon. */
    def setLatLon(latLng2: LatLng): Unit = {
      for (ilat <- lat) {
        ilat.value = latLng2.lat
      }
      for (ilon <- lon) {
        ilon.value = latLng2.lng
      }
    }

    def latLngOpt: Option[LatLng] = {
      for {
        inpLat  <- lat
        lat     <- inpLat.value
        inpLon  <- lon
        lon     <- inpLon.value
      } yield {
        L.latLng(lat, lng = lon)
      }
    }

  }

}


trait MapStateInputs extends InputsHelpers {

  object _map extends SetLatLon {
    override lazy val lat       = _find( InpMapLat )
    override lazy val lon       = _find( InpMapLon )
    lazy val zoom               = _find( InpMapZoom )
  }

}
