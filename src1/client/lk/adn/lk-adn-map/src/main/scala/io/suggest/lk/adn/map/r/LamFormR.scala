package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adn.map.m.{MLamRad, MLamRcvrs, MRoot}
import io.suggest.maps.m.{MExistGeoS, MGeoMapPropsR}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import io.suggest.css.Css
import io.suggest.maps.r.{LGeoMapR, ReactLeafletUtil}
import react.leaflet.control.LocateControlR
import io.suggest.bill.price.dsl.PriceDsl
import io.suggest.lk.adv.r.{Adv4FreeR, ItemsPricesR}
import io.suggest.sjs.dt.period.r.DatePeriodR
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.common.empty.OptionUtil
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
object LamFormR {

  import MGeoMapPropsR.MGeoMapPropsRFastEq
  import MLamRcvrs.MLamRcvrsFastEq
  import MExistGeoS.MExistGeoSFastEq
  import RadPopupR.PropsValFastEq

  type Props = ModelProxy[MRoot]


  /** Модель состояния компонента. */
  protected[this] case class State(
                                    geoMapPropsC          : ReactConnectProxy[MGeoMapPropsR],
                                    radOptsC              : ReactConnectProxy[MLamRad],
                                    rcvrsC                : ReactConnectProxy[MLamRcvrs],
                                    priceDslOptC          : ReactConnectProxy[Option[Tree[PriceDsl]]],
                                    currentPotC           : ReactConnectProxy[MExistGeoS],
                                    radPopupPropsC        : ReactConnectProxy[Option[RadPopupR.PropsVal]]
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
        s.rcvrsC { rcvrsProxy =>

          // Рендерить текущий размещения и rad-маркер всегда в верхнем слое:
          val lg = List[VdomElement](
            // Рендер текущих размещений.
            s.currentPotC { CurrentGeoR.component.apply },
            // Маркер местоположения узла.
            s.radOptsC { MapCursorR.component.apply }
          )

          rcvrsProxy().nodesResp.toOption.fold[VdomElement] {
            LayerGroupR()( lg: _* )
          } { _ =>
            LamRcvrsR(rcvrsProxy)( lg: _* )
          }
        },

        // L-попап при клике по rad cursor.
        s.radPopupPropsC { RadPopupR.component.apply },

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
          OptsR(p)
        ),

        // Верхняя половина, правая колонка:
        p.wrap(_.datePeriod)( DatePeriodR.component.apply ),

        // Рендер географической карты:
        s.geoMapPropsC { mapPropsProxy =>
          LMapR.component(
            LGeoMapR.lmMapSProxy2lMapProps( mapPropsProxy, lgmCtx )
          )( mapChildren: _* )
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
      val mapCssClass = Some( Css.Lk.Maps.MAP_CONTAINER )
      State(
        geoMapPropsC         = propsProxy.connect { p =>
          MGeoMapPropsR(
            mapS          = p.mmap,
            cssClass      = mapCssClass
          )
        },
        radOptsC      = propsProxy.connect(_.rad),
        rcvrsC        = propsProxy.connect(_.rcvrs),
        priceDslOptC  = propsProxy.connect(_.price.respDslOpt),
        currentPotC   = propsProxy.connect(_.current),
        radPopupPropsC = propsProxy.connect { p =>
          OptionUtil.maybe( p.rad.popup ) {
            RadPopupR.PropsVal(
              point = p.rad.circle.center
            )
          }
        }
      )
    }
    .renderBackend[Backend]
    .build

}
