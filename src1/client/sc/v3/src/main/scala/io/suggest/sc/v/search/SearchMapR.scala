package io.suggest.sc.v.search

import diode.FastEq
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.{MGeoLoc, MGeoPoint}
import io.suggest.maps.m.{HandleMapReady, MGeoMapPropsR, MMapS, MapDragEnd}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.userloc.UlShapeR
import io.suggest.maps.r.{LGeoMapR, MapLoaderMarkerR, RcvrMarkersR, ReactLeafletUtil}
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
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.11.17 21:52
  * Description: Компонент географической карты для панели поиска.
  *
  * Рендерится ЧЕРЕЗ wrap(), connect() будет тормозить сильно.
  * Содержит в себе компоненты для карты и всего остального.
  */
class SearchMapR(
                  getScCssF  : GetScCssF
                ) {

  import MGeoMapPropsR.MGeoMapPropsRFastEq
  import MMapS.MMapSFastEq4Map


  case class PropsVal(
                       searchCss    : SearchCss,
                       mapInit      : MMapInitState
                     )
  implicit object SearchMapRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.searchCss ===* b.searchCss) &&
        (a.mapInit ===* b.mapInit)
    }
  }

  protected[this] case class State(
                                    mmapC       : ReactConnectProxy[MMapS],
                                    rcvrsGeoC   : ReactConnectProxy[Pot[MGeoNodesResp]],
                                    loaderOptC  : ReactConnectProxy[Option[MGeoPoint]],
                                    userLocOptC : ReactConnectProxy[Option[MGeoLoc]],
                                  )


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, State]) {

    private def _onMapDragEnd(e: DragEndEvent): Callback = {
      dispatchOnProxyScopeCB( $, MapDragEnd(distancePx = e.distance) )
    }
    private val _onMapDragEndOptF = Some( ReactCommonUtil.cbFun1ToJsCb( _onMapDragEnd ) )


    private def _onMapReady(e: IWhenReadyArgs): Callback = {
      dispatchOnProxyScopeCB( $, HandleMapReady(e.target) )
    }
    private val _onMapReadyOptF = {
      Some( ReactCommonUtil.cbFun1ToJsCb(_onMapReady) )
    }


    def render(propsProxy: Props, s: State): VdomElement = {
      val props = propsProxy.value

      // Все компоненты инициализируются с lazy, т.к. раньше встречались какие-то рандомные ошибки в некоторых слоях. race conditions?

      // Рендерим основную гео-карту:
      lazy val tileLayer = ReactLeafletUtil.Tiles.OsmDefault
      // Плагин для геолокации текущего юзера.
      lazy val locateControl = LocateControlR()
      // Рендер шейпов и маркеров текущий узлов.
      lazy val rcvrsGeo = s.rcvrsGeoC { RcvrMarkersR.applyNoChildren }
      // Рендер опционального маркера-крутилки для ожидания загрузки.
      lazy val loaderOpt = s.loaderOptC { MapLoaderMarkerR.component.apply }
      // Рендер круга текущей геолокации юзера:
      lazy val userLoc = s.userLocOptC { UlShapeR.component.apply }

      // Рендер компонента leaflet-карты вне maybeEl чтобы избежать перерендеров.
      // Вынос этого компонента за пределы maybeEl() поднял производительность карты на порядок.
      lazy val mmapComp = {
        val mapCSS = getScCssF().Search.Tabs.MapTab
        val geoMapCssSome = Some(
          (mapCSS.geomap.htmlClass :: props.searchCss.GeoMap.geomap.htmlClass :: Nil)
            .mkString( HtmlConstants.SPACE )
        )
        //val someFalse = Some(false)

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
              userLoc,
              rcvrsGeo,
              loaderOpt
            )
          }
        }
      }

      // Наконец, непосредственный рендер карты:
      ReactCommonUtil.maybeEl(props.mapInit.ready) {
        mmapComp
      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mapInitProxy =>
      State(
        mmapC       = mapInitProxy.connect(_.mapInit.state),
        rcvrsGeoC   = mapInitProxy.connect { props =>
          // Отображать найденные в поиске ресиверы вместо всех.
          props
            .mapInit.rcvrs
            .map(_.resp)
        },
        loaderOptC  = mapInitProxy.connect(_.mapInit.loader)( OptFastEq.Plain ),
        userLocOptC = mapInitProxy.connect { props =>
          props
            .mapInit
            .userLoc
            // Запретить конфликты шейпов с LocationControl-плагином. TODO Удалить плагин, геолокация полностью должна жить в состоянии и рендерится только отсюда.
            .filter { _ =>
              props.mapInit.state.locationFound.isEmpty
            }
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(mapInitProxy: Props) = component(mapInitProxy)

}
