package io.suggest.maps.r

import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.maps.u.{MapIcons, MapsUtil}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.18 11:18
  * Description: Маркер Leaflet с крулкой, обозначающей подгрузку.
  */
object MapLoaderMarkerR {

  val component = ScalaComponent
    .builder[ModelProxy[Option[MGeoPoint]]]( getClass.getSimpleName )
    .stateless
    .render_P { geoPointOptProxy =>
      geoPointOptProxy.value.whenDefinedEl { mgp =>
        MapIcons.preloaderLMarker(
          latLng = MapsUtil.geoPoint2LatLng( mgp )
        )
      }
    }
    .build

}
