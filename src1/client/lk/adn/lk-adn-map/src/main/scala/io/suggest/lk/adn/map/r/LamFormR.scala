package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adn.map.m.{MLamRcvrs, MRoot}
import io.suggest.maps.m.{MExistGeoS, MGeoMapPropsR, MMapS}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import io.suggest.css.Css
import io.suggest.maps.r.{LGeoMapR, ReactLeafletUtil}
import react.leaflet.control.LocateControlR
import io.suggest.bill.price.dsl.PriceDsl
import io.suggest.lk.adv.r.{Adv4FreeR, ItemsPricesR}
import io.suggest.sjs.dt.period.r.DatePeriodR
import react.leaflet.layer.LayerGroupR
import react.leaflet.lmap.LMapR
import japgolly.scalajs.react.vdom.html_<^._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:31
  * Description: top-level компонент для react-экранизации формы размещения узла на карте.
  *
  * Все react-компоненты были наготово взяты из lk-adv-geo-формы, как и в прошлый раз
  * на fsm-mvm архитектуре.
  */
final class LamFormR(
                      optsR: OptsR,
                      radPopupR: RadPopupR,
                      mapCursorR: MapCursorR,
                      lamRcvrsR: LamRcvrsR,
                      currentGeoR: CurrentGeoR,
                    ) {

  import MGeoMapPropsR.MGeoMapPropsRFastEq
  import MLamRcvrs.MLamRcvrsFastEq
  import MExistGeoS.MExistGeoSFastEq

  type Props = ModelProxy[MRoot]


  /** Модель состояния компонента. */
  protected[this] case class State(
                                    geoMapPropsC          : ReactConnectProxy[MMapS],
                                    rcvrsC                : ReactConnectProxy[MLamRcvrs],
                                    priceDslOptC          : ReactConnectProxy[Option[Tree[PriceDsl]]],
                                  )


  /** Логика рендера компонента всея формы. */
  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {

      val mapChildren = List[VdomElement](

        // Рендерим основную плитку карты.
        ReactLeafletUtil.Tiles.OsmDefault,

        // Плагин для геолокации текущего юзера.
        LocateControlR(),

        // Карта покрытия ресиверов (подгружается асихронно).
        {
          // Рендерить текущий размещения и rad-маркер всегда в верхнем слое:
          val lg = List[VdomElement](
            // Рендер текущих размещений.
            p.wrap( _.current )( currentGeoR.component.apply ),
            // Маркер местоположения узла.
            p.wrap( _.rad )( mapCursorR.component.apply ),
          )
          s.rcvrsC { rcvrsProxy =>
            rcvrsProxy()
              .nodesResp
              .toOption
              .fold[VdomElement] {
                LayerGroupR()( lg: _* )
              } { _ =>
                lamRcvrsR.component(rcvrsProxy)( lg: _* )
              }
          }
        },

        // L-попап при клике по rad cursor.
        p.wrap( _.rad )( radPopupR.component.apply ),

      )

      val lgmCtx = LGeoMapR.LgmCtx.mk( $ )

      <.div(
        ^.`class` := Css.Lk.Adv.FORM_OUTER_DIV,

        // Плашка с галочкой бесплатного размещения для суперюзеров.
        p.wrap(_.adv4free)( Adv4FreeR.component.apply ),

        // Верхняя половина, левая колонка
        <.div(
          ^.`class` := Css.Lk.Adv.LEFT_BAR,
          // Для OptsR можно использовать wrap, но этот же коннекшен пробрасывается в rad popup, поэтому везде connect.
          optsR.component( p )
        ),

        // Верхняя половина, правая колонка:
        p.wrap(_.datePeriod)( DatePeriodR.component.apply ),

        // Рендер географической карты:
        {
          val mapCssClass = Some( Css.Lk.Maps.MAP_CONTAINER )
          s.geoMapPropsC { mapPropsProxy =>
            val lgmPropsProxy = mapPropsProxy.zoom { mmapS =>
              MGeoMapPropsR(
                mapS          = mmapS,
                cssClass      = mapCssClass
              )
            }
            LMapR.component(
              LGeoMapR.lmMapSProxy2lMapProps( lgmPropsProxy, lgmCtx )
            )( mapChildren: _* )
          }
        },


        <.br,

        // Рендерить табличку с данными по рассчёту текущей цены:
        s.priceDslOptC { ItemsPricesR.component.apply }

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        geoMapPropsC  = propsProxy.connect( _.mmap ),
        rcvrsC        = propsProxy.connect(_.rcvrs),
        priceDslOptC  = propsProxy.connect(_.price.respDslOpt),
      )
    }
    .renderBackend[Backend]
    .build

}
