package io.suggest.maps.r

import diode.react.ModelProxy
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil
import io.suggest.react.{Props2ModelProxy, ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.sjs.leaflet.event.{Event, LocationEvent, PopupEvent}
import io.suggest.sjs.leaflet.map.LMap
import japgolly.scalajs.react.BackendScope
import react.leaflet.lmap.LMapPropsR

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 17:57
  * Description: React-компонент карты георазмещения.
  * По сути -- обёртка вокруг react-leaflet и diode.
  */
object LGeoMapR {

  /** Внешний контекст, чтобы инстансы кэшировать. */
  case class LgmCtx(
                     onLocationFoundF   : js.Function1[LocationEvent, Unit],
                     onPopupCloseF      : js.Function1[PopupEvent, Unit],
                     onZoomEndF         : js.Function1[Event, Unit],
                     onMoveEndF         : js.Function1[Event, Unit],
                     attribution        : js.UndefOr[Boolean],
                   )
  object LgmCtx {
    def mk[P: Props2ModelProxy, S]($: BackendScope[P, S],
                                   attribution: js.UndefOr[Boolean] = js.undefined) = LgmCtx(
      onLocationFoundF = ReactCommonUtil.cbFun1ToJsCb { locEvent: LocationEvent =>
        val gp = MapsUtil.latLng2geoPoint( locEvent.latLng )
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, HandleLocationFound(gp) )
      },

      onPopupCloseF = ReactCommonUtil.cbFun1ToJsCb { _: PopupEvent =>
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, HandleMapPopupClose )
      },

      onZoomEndF = ReactCommonUtil.cbFun1ToJsCb { event: Event =>
        val newZoom = event.target.asInstanceOf[LMap].getZoom().toInt
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, MapZoomEnd(newZoom) )
      },

      onMoveEndF = ReactCommonUtil.cbFun1ToJsCb { event: Event =>
        val newZoom = event.target.asInstanceOf[LMap].getCenter()
        ReactDiodeUtil.dispatchOnProxyScopeCB($, MapMoveEnd(newZoom) )
      },

      attribution = attribution,
    )
  }


  /** Сгенерить пропертисы для LGeoMapR для типичной ситуации отображения карты в ЛК.
    *
    * @param proxy Прокси [[io.suggest.maps.m.MGeoMapPropsR]].
    * @param lgmCtx Постоянные инстансы, хранящиеся за пределами map-коннекшена.
    * @return Инстанс LMapPropsR.
    */
  def lmMapSProxy2lMapProps( proxy: ModelProxy[MGeoMapPropsR], lgmCtx: LgmCtx ): LMapPropsR = {
    val v = proxy()
    val _onLocationFound2 = JsOptionUtil.maybeDefined( v.mapS.locationFound contains[Boolean] true ) {
      lgmCtx.onLocationFoundF
    }
    // Карта должна рендерится с такими параметрами:
    new LMapPropsR {
      override val center    = MapsUtil.geoPoint2LatLng( v.mapS.center )
      override val zoom      = js.defined( v.mapS.zoom )

      // maxZoom: Значение требует markercluster
      // 18 - в leaflet бывает сверхприближение на touch-устройствах с retina, и пустая карта.
      // 17 - видны номера домов в городе на osm.org.
      // 16 - номера домов не видны.
      override val maxZoom   = js.defined( 17 )

      override val useFlyTo  = v.animated
      override val onLocationFound = _onLocationFound2

      override val onPopupClose = js.defined( lgmCtx.onPopupCloseF )
      override val onZoomEnd    = js.defined( lgmCtx.onZoomEndF )
      override val onMoveEnd    = js.defined( lgmCtx.onMoveEndF )

      // Пробрасываем extra-пропертисы:
      override val whenReady    = v.whenReady.toUndef
      override val className    = v.cssClass.toUndef
      override val onDragStart  = v.onDragStart.toUndef
      override val onDragEnd    = v.onDragEnd.toUndef
      override val attributionControl = lgmCtx.attribution
    }
  }

}

