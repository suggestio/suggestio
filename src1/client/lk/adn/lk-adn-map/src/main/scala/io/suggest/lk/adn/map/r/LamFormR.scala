package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adn.map.m.{MNodeMarkerS, MRoot}
import io.suggest.maps.m.MMapS
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.css.Css
import io.suggest.lk.adn.map.r.nm.NodeMarkerR
import io.suggest.maps.r.{LGeoMapR, ReactLeafletUtil}
import react.leaflet.control.LocateControlR
import MMapS.MMapSFastEq
import MNodeMarkerS.MNodeMarkerFastEq
import io.suggest.bill.price.dsl.IPriceDslTerm
import io.suggest.i18n.MsgCodes
import io.suggest.lk.adv.r.{Adv4FreeR, ItemsPricesR}
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.dt.period.r.DatePeriodR

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

  type Props = ModelProxy[MRoot]


  /** Модель состояния компонента. */
  protected[this] case class State(
                                    mmapC                 : ReactConnectProxy[MMapS],
                                    nodeMarkerC           : ReactConnectProxy[MNodeMarkerS],
                                    priceDslOptC          : ReactConnectProxy[Option[IPriceDslTerm]]
                                  )


  /** Логика рендера компонента всея формы. */
  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): ReactElement = {
      <.div(
        ^.`class` := Css.Lk.Adv.FORM_OUTER_DIV,

        // Плашка с галочкой бесплатного размещения для суперюзеров.
        p.wrap(_.adv4free) { a4fOptProx =>
          Adv4FreeR(a4fOptProx)
        },

        // Верхняя половина, левая колонка:
        <.div(
          ^.`class` := Css.Lk.Adv.LEFT_BAR,
          Messages( MsgCodes.`You.can.place.adn.node.on.map.below` )
        ),

        // Верхняя половина, правая колонка:
        p.wrap(_.datePeriod)( DatePeriodR.apply ),


        // Рендер географической карты:
        s.mmapC { mapProps =>
          LGeoMapR(mapProps)(

            // Рендерим основную плитку карты.
            ReactLeafletUtil.Tiles.OsmDefault,

            // Плагин для геолокации текущего юзера.
            LocateControlR()(),

            // Маркер местоположения узла.
            s.nodeMarkerC { NodeMarkerR.apply }

          )
        },


        <.br,

        // Рендерить табличку с данными по рассчёту текущей цены:
        s.priceDslOptC { ItemsPricesR.apply }

      )
    }

  }


  val component = ReactComponentB[Props]("LamForm")
    .initialState_P { p =>
      State(
        mmapC         = p.connect(_.mmap),
        nodeMarkerC   = p.connect(_.nodeMarker),
        priceDslOptC  = p.connect(_.price.respDslOpt)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(mRootProxy: Props) = component(mRootProxy)

}
