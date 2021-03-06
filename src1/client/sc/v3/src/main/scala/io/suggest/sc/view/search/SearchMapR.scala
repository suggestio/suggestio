package io.suggest.sc.view.search

import diode.FastEq
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.geo.{MGeoLoc, MGeoPoint}
import io.suggest.maps.{MMapS, MapDragEnd}
import io.suggest.maps.m._
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.userloc.LocShapeR
import io.suggest.maps.r.{LGeoMapR, MapLoaderMarkerR, RcvrMarkersR, ReactLeafletUtil}
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.model.search.{MGeoTabS, MSearchRespInfo}
import io.suggest.sc.view.styl.ScCssStatic
import io.suggest.sjs.leaflet.control.locate.LocateControlOptions
import io.suggest.sjs.leaflet.event.DragEndEvent
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.js.react.leaflet.MapContainer
import io.suggest.ueq.JsUnivEqUtil._
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
final class SearchMapR {

  implicit val resizeMapStateFeq = FastEqUtil[Props_t] { (a, b) =>
    a.css ===* b.css
  }

  protected[this] case class State(
                                    mmapC       : ReactConnectProxy[Props_t],
                                    rcvrsGeoC   : ReactConnectProxy[Pot[MSearchRespInfo[MGeoNodesResp]]],
                                    loaderOptC  : ReactConnectProxy[Option[MGeoPoint]],
                                    userLocOptC : ReactConnectProxy[Option[MGeoLoc]],
                                    isInitSomeC : ReactConnectProxy[Some[Boolean]],
                                    resizeMapC  : ReactConnectProxy[Props_t],
                                  )


  type Props_t = MGeoTabS
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, State]) {

    private val _onMapDragEndOptF = ReactCommonUtil.cbFun1ToJsCb { e: DragEndEvent =>
      dispatchOnProxyScopeCB( $, MapDragEnd(distancePx = e.distance) )
    }

    def render(propsProxy: Props, s: State): VdomElement = {
      // Рендер компонента leaflet-карты вне maybeEl чтобы избежать перерендеров.
      // Вынос этого компонента за пределы maybeEl() поднял производительность карты на порядок.
      lazy val mmapComp = {
        val _stopPropagationF = ReactCommonUtil.stopPropagationCB _
        val props = propsProxy.value

        val lgmCtx = LGeoMapR.LgmCtx(
          propsProxy,
          onDragEnd = _onMapDragEndOptF,
        )

        val mapChildren = List[VdomElement](
          // Рендерим основную гео-карту:
          ReactLeafletUtil.Tiles.OsmDefault,

          // Плагин для геолокации текущего юзера.
          lgmCtx.LocateControlR(
            new LocateControlOptions {
              // iOS=false: showCompass требует разрешение на motion detection, которое вылетает каждый раз и не сохраняется.
              override val showCompass = false
            }
          ),
          lgmCtx.EventsR(),

          // Рендер шейпов и маркеров текущий узлов.
          s.rcvrsGeoC { reqWrapProxy =>
            reqWrapProxy.wrap( _.map(_.resp) )( RcvrMarkersR.component(_)() )
          },

          // Рендер опционального маркера-крутилки для ожидания загрузки.
          s.loaderOptC { MapLoaderMarkerR.component.apply },

          // Рендер круга текущей геолокации юзера:
          s.userLocOptC { LocShapeR.component.apply },

          // Рендер поддержки обновления координат и зума карты:
          s.mmapC { geoTabProxy =>
            val geoTab = geoTabProxy.value
            LGeoMapR.CenterZoomTo(
              MGeoMapPropsR(
                mapS      = geoTab.mapInit.state,
                // Управление анимацией: при наличии каких-либо ресиверов, нужно вырубать анимацию, чтобы меньше дёргалась карта при поиске.
                animated  = geoTab.isRcvrsEqCached,
              )
            )
          },

          s.resizeMapC { _ =>
            LGeoMapR.InvalidateMapSize()
          },

        )

        // Код сборки css был унесён за пределы тела коннекшена до внедрения scCss-через-контекст.
        // Нахождение этого кода здесь немного снижает производительность.
        val geoMapCssSome = Some(
          (ScCssStatic.Search.Geo.geomap :: props.css.GeoMap.geomap :: Nil)
            .toHtmlClass
        )

        <.div(
          ^.onTouchStart  ==> _stopPropagationF,
          ^.onTouchEnd    ==> _stopPropagationF,
          ^.onTouchMove   ==> _stopPropagationF,
          ^.onTouchCancel ==> _stopPropagationF,

          propsProxy.wrap(_.mapInit.state) { mmapProxy =>
            MapContainer.component(
              LGeoMapR.reactLeafletMapProps(
                MGeoMapPropsR(
                  mapS          = mmapProxy.value,
                  // Управление анимацией: при наличии каких-либо ресиверов, нужно вырубать анимацию, чтобы меньше дёргалась карта при поиске.
                  animated      = propsProxy.value.isRcvrsEqCached,
                  cssClass      = geoMapCssSome,
                ),
                lgmCtx,
              )
            )( mapChildren: _* )
          },

        )
      }

      // Наконец, непосредственный рендер карты:
      s.isInitSomeC { someReadyProxy =>
        ReactCommonUtil.maybeEl( someReadyProxy.value.value ) {
          mmapComp
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mapInitProxy =>
      State(

        mmapC = mapInitProxy.connect( identity )( FastEqUtil[MGeoTabS] {(a, b) =>
          MMapS.CenterZoomFeq.eqv( a.mapInit.state, b.mapInit.state )
        }),

        rcvrsGeoC   = mapInitProxy.connect { props =>
          // Отображать найденные в поиске ресиверы вместо всех.
          props.mapInit.rcvrs
        }( FastEqUtil.AnyRefFastEq ),

        loaderOptC  = mapInitProxy.connect(_.mapInit.loader)( OptFastEq.Plain ),

        userLocOptC = mapInitProxy.connect { props =>
          props
            .mapInit
            .userLoc
            // Запретить конфликты шейпов с LocationControl-плагином. TODO Удалить плагин, геолокация полностью должна жить в состоянии и рендерится только отсюда.
            .filter { _ =>
              props.mapInit.state.locationFound.isEmpty
            }
        },

        isInitSomeC = mapInitProxy.connect { props =>
          OptionUtil.SomeBool( props.mapInit.ready )
        }( FastEq.AnyRefEq ),

        resizeMapC = mapInitProxy.connect( identity )( resizeMapStateFeq ),

      )
    }
    .renderBackend[Backend]
    .build

}
