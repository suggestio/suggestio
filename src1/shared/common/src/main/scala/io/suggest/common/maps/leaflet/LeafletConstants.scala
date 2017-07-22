package io.suggest.common.maps.leaflet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:38
  * Description: Константы для работы с Leaflet.
  */
object LeafletConstants {

  object Tiles {

    /** Ссылка на дефолтовые тайлы карты.
      *
      * 2017.jul.22: Выставлен принудительный https. На голом http российский кэш Squid на
      * gorynych.openstreetmap.org возвращал HTTP 500: Socket Failure: Address already in use.
      */
    def URL_OSM_DFLT    = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"

    def ATTRIBUTION_OSM = """&copy; <a href="//www.openstreetmap.org/copyright">OpenStreetMap</a> contributors"""

  }

}
