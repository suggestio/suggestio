package io.suggest.sc.v.search

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProps, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m.{HandleMapReady, MGeoMapPropsR, MMapS, MapDragEnd}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.{LGeoMapR, RcvrMarkersR, ReactLeafletUtil}
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.MMapInitState
import io.suggest.sc.styl.GetScCssF
import io.suggest.sjs.leaflet.event.DragEndEvent
import io.suggest.sjs.leaflet.map.IWhenReadyArgs
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import react.leaflet.control.LocateControlR
import react.leaflet.lmap.LMapR
import io.suggest.spa.OptFastEq
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.11.17 21:52
  * Description: Компонент географической карты для панели поиска.
  */
class SearchMapR(
                  getScCssF  : GetScCssF
                ) {

  import MGeoMapPropsR.MGeoMapPropsRFastEq
  import MMapS.MMapSFastEq4Map


  protected[this] case class State(
                                    mmapC       : ReactConnectProxy[MMapS],
                                    rcvrsGeoC   : ReactConnectProxy[Pot[MGeoNodesResp]],
                                    loaderOptC  : ReactConnectProxy[Option[MGeoPoint]]
                                  )


  type Props_t = MMapInitState
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, State]) {

    private def _onMapDragEnd(e: DragEndEvent): Callback = {
      dispatchOnProxyScopeCB( $, MapDragEnd(distancePx = e.distance) )
    }
    private val _onMapDragEndOptF = Some( ReactCommonUtil.cbFun1ToJsCb( _onMapDragEnd ) )


    private val _mapLoaderReuseF = { loaderOpt: ModelProxy[Option[MGeoPoint]] =>
      loaderOpt.value.whenDefinedEl { mgp =>
        MapIcons.preloaderLMarker(
          latLng = MapsUtil.geoPoint2LatLng( mgp )
        )
      }
    }


    private def _onMapReady(e: IWhenReadyArgs): Callback = {
      dispatchOnProxyScopeCB( $, HandleMapReady(e.target) )
    }
    private val _onMapReadyOptF = {
      Some( ReactCommonUtil.cbFun1ToJsCb(_onMapReady) )
    }


    def render(mapInitProxy: Props, s: State): VdomElement = {
      val mapCSS = getScCssF().Search.Tabs.MapTab
      val _stopPropagationF = ReactCommonUtil.stopPropagationCB _

      val mapInit = mapInitProxy.value

      <.div(
        mapCSS.outer,

        <.div(
          mapCSS.wrapper,

          <.div(
            mapCSS.inner,
            ^.onTouchStart  ==> _stopPropagationF,
            ^.onTouchEnd    ==> _stopPropagationF,
            ^.onTouchMove   ==> _stopPropagationF,
            ^.onTouchCancel ==> _stopPropagationF,

            {
              // Рендерим основную гео-карту:
              lazy val tileLayer = ReactLeafletUtil.Tiles.OsmDefault
              // Плагин для геолокации текущего юзера.
              lazy val locateControl = LocateControlR()
              // Рендер шейпов и маркеров текущий узлов.
              lazy val rcvrsGeo = s.rcvrsGeoC { RcvrMarkersR.applyNoChildren }
              // Рендер опционального маркера-крутилки для ожидания загрузки.
              lazy val loaderOpt = s.loaderOptC { _mapLoaderReuseF }

              ReactCommonUtil.maybeEl(mapInit.ready) {
                val geoMapCssSome = Some( mapCSS.geomap.htmlClass )
                //val someFalse = Some(false)

                // TODO Нужно как-то организовать reuse инстанса фунции. Эта фунция зависит от state, и хз, как это нормально организовать. Вынести в top-level?
                s.mmapC { mmapProxy =>
                  mmapProxy.wrap { mmap =>
                    MGeoMapPropsR(
                      center        = mmap.center,
                      zoom          = mmap.zoom,
                      locationFound = mmap.locationFound,
                      cssClass      = geoMapCssSome,
                      // Вручную следим за ресайзом, т.к. у Leaflet это плохо получается (если карта хоть иногда бывает ЗА экраном, его считалка размеров ломается).
                      //trackWndResize = someFalse,
                      whenReady     = _onMapReadyOptF,
                      //onDragStart   = _onMapDragStartOptF,
                      onDragEnd     = _onMapDragEndOptF
                    )
                  } { geoMapPropsProxy =>
                    LMapR(
                      LGeoMapR
                        .lmMapSProxy2lMapProps( geoMapPropsProxy )
                        .noAttribution
                    )(
                      tileLayer,
                      locateControl,
                      rcvrsGeo,
                      loaderOpt
                    )
                  }

                }
              }
            }

          )
        ),

        // Прицел для наведения. Пока не ясно, отображать его всегда или только когда карта перетаскивается.
        <.div(
          mapCSS.crosshair,
          HtmlConstants.PLUS
        )

      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mapInitProxy =>
      State(
        mmapC       = mapInitProxy.connect(_.state),
        rcvrsGeoC   = mapInitProxy.connect(_.rcvrsGeo),
        loaderOptC  = mapInitProxy.connect(_.loader)( OptFastEq.Plain )
      )
    }
    .renderBackend[Backend]
    .build

  private def _apply(mapInitProxy: Props) = component(mapInitProxy)
  val apply: ReactConnectProps[Props_t] = _apply

}
