package io.suggest.maps.r

import diode.react.ModelProxy
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil
import io.suggest.react.ReactCommonUtil
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.sjs.leaflet.control.locate.{LocateControl, LocateControlOptions}
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event, Events, LeafletEventHandlerFnMap, LocationEvent, LocationEventHandlerFn, PopupEvent}
import io.suggest.sjs.leaflet.map.LMap
import japgolly.scalajs.react.component.ReactForwardRef
import japgolly.scalajs.react.ScalaFnComponent
import org.js.react.leaflet.{LocateControl, MapContainerProps, useMap, useMapEvent, useMapEvents}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 17:57
  * Description: React-компонент карты георазмещения.
  * По сути -- обёртка вокруг react-leaflet и diode.
  */
object LGeoMapR {

  case class LgmCtx(
                     proxy          : ModelProxy[_],
                     onDragEnd      : js.UndefOr[js.Function1[DragEndEvent, Unit]]  = js.undefined,
                   ) {

    /** Компонент LocateControl с подпиской на события карты. */
    object LocateControlR {
      val component = ReactForwardRef[LocateControlOptions, LocateControl] { (props, refOpt) =>
        // Подписаться на locationfound-события карты:
        useMapEvent(
          Events.LOCATION_FOUND,
          ReactCommonUtil.cbFun1ToJsCb { locEvent: LocationEvent =>
            val gp = MapsUtil.latLng2geoPoint( locEvent.latLng )
            proxy.dispatchCB( HandleLocationFound(gp) )
          }: LocationEventHandlerFn
        )

        // Вернуть компонент LocationControl:
        LocateControl.component.withOptionalRef( refOpt )( props )
      }
      def apply(props: LocateControlOptions = new LocateControlOptions {}) =
        component(props)
    }


    /** Подписка на основные события гео.карты. */
    val EventsR = ScalaFnComponent[Unit] { _ =>
      useMapEvents(
        new LeafletEventHandlerFnMap {
          override val popupclose = ReactCommonUtil.cbFun1ToJsCb { e: PopupEvent =>
            proxy.dispatchCB( HandleMapPopupClose )
          }
          override val zoomend = ReactCommonUtil.cbFun1ToJsCb { event: Event =>
            val newZoom = event.target.asInstanceOf[LMap].getZoom().toInt
            proxy.dispatchCB( MapZoomEnd(newZoom) )
          }
          override val moveend = ReactCommonUtil.cbFun1ToJsCb { event: Event =>
            val newZoom = event.target.asInstanceOf[LMap].getCenter()
            proxy.dispatchCB( MapMoveEnd(newZoom) )
          }
        }
      )

      // useMapEvents() плохо отрабатывает undefined-значения. Поэтому вручную проходим опциональные листенеры.
      for (fn <- onDragEnd)
        useMapEvent( Events.DRAG_END, fn )

      // Без компонента, только эффект подписки на события.
      ReactCommonUtil.VdomNullElement
    }

  }


  /** Сгенерить пропертисы для LGeoMapR для типичной ситуации отображения карты в ЛК.
    *
    * @param proxy Прокси [[io.suggest.maps.m.MGeoMapPropsR]].
    * @param lgmCtx Постоянные инстансы, хранящиеся за пределами map-коннекшена.
    * @return Инстанс LMapPropsR.
    */
  def reactLeafletMapProps( v: MGeoMapPropsR, lgmCtx: LgmCtx ): MapContainerProps = {
    new MapContainerProps {
      override val center         = MapsUtil.geoPoint2LatLng( v.mapS.center )
      override val zoom           = js.defined( v.mapS.zoom )

      // maxZoom: Значение требует markercluster
      // 18 - в leaflet бывает сверхприближение на touch-устройствах с retina, и пустая карта.
      // 17 - видны номера домов в городе на osm.org.
      // 16 - номера домов не видны.
      override val maxZoom        = js.defined( 17 )
      override val whenCreated    = v.whenCreated.toUndef
      override val className      = v.cssClass.toUndef

      override val attributionControl = v.attribution.orUndefined
    }
  }


  lazy val CenterZoomTo = ScalaFnComponent[MGeoMapPropsR] { props =>
    val mapInstance = useMap()

    val centerLL = MapsUtil.geoPoint2LatLng( props.mapS.center )
    val zoom = props.mapS.zoom

    if (props.animated)
      mapInstance.setView( centerLL, zoom )
    else
      mapInstance.flyTo( centerLL, zoom )

    ReactCommonUtil.VdomNullElement
  }

}

