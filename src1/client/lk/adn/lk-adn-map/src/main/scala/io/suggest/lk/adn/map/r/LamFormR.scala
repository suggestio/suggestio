package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adn.map.m.{IRadOpts, MLamRcvrs, MRoot}
import io.suggest.maps.m.{MExistGeoS, MMapS}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import io.suggest.css.Css
import io.suggest.maps.r.{LGeoMapR, LMapExtraProps, ReactLeafletUtil}
import react.leaflet.control.LocateControlR
import MMapS.MMapSFastEq
import io.suggest.bill.price.dsl.IPriceDslTerm
import io.suggest.lk.adv.r.{Adv4FreeR, ItemsPricesR}
import io.suggest.sjs.dt.period.r.DatePeriodR
import IRadOpts.IRadOptsFastEq
import MExistGeoS.MExistGeoSFastEq
import io.suggest.spa.OptFastEq.Wrapped
import RadPopupR.PropsValFastEq
import io.suggest.common.empty.OptionUtil
import react.leaflet.layer.LayerGroupR
import react.leaflet.lmap.LMapR
import japgolly.scalajs.react.vdom.html_<^._

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
                                    radOptsC              : ReactConnectProxy[IRadOpts[_]],
                                    rcvrsC                : ReactConnectProxy[MLamRcvrs],
                                    priceDslOptC          : ReactConnectProxy[Option[IPriceDslTerm]],
                                    currentPotC           : ReactConnectProxy[MExistGeoS],
                                    radPopupPropsC        : ReactConnectProxy[Option[RadPopupR.PropsVal]]
                                  )


  /** Логика рендера компонента всея формы. */
  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.div(
        ^.`class` := Css.Lk.Adv.FORM_OUTER_DIV,

        // Плашка с галочкой бесплатного размещения для суперюзеров.
        p.wrap(_.adv4free) { a4fOptProx =>
          Adv4FreeR(a4fOptProx)
        },

        // Верхняя половина, левая колонка
        <.div(
          ^.`class` := Css.Lk.Adv.LEFT_BAR,
          // Для OptsR можно использовать wrap, но этот же коннекшен пробрасывается в rad popup, поэтому везде connect.
          OptsR(p)
        ),

        // Верхняя половина, правая колонка:
        p.wrap(_.datePeriod)( DatePeriodR.apply ),

        // Рендер географической карты:
        s.mmapC { mapPropsProxy =>
          val lMapProps = LGeoMapR.lmMapSProxy2lMapProps(
            mapPropsProxy,
            LMapExtraProps(cssClass = Css.Lk.Maps.MAP_CONTAINER)
          )
          LMapR(lMapProps)(

            // Рендерим основную плитку карты.
            ReactLeafletUtil.Tiles.OsmDefault,

            // Плагин для геолокации текущего юзера.
            LocateControlR(),


            // Карта покрытия ресиверов (подгружается асихронно).
            s.rcvrsC { rcvrsProxy =>

              // Рендерить текущий размещения и rad-маркер всегда в верхнем слое:
              val lg = List[VdomElement](
                // Рендер текущих размещений.
                s.currentPotC { CurrentGeoR.apply },
                // Маркер местоположения узла.
                s.radOptsC { MapCursorR.apply }
              )

              rcvrsProxy().nodesResp.toOption.fold[VdomElement] {
                LayerGroupR()( lg: _* )
              } { _ =>
                LamRcvrsR(rcvrsProxy)( lg: _* )
              }
            },

            // L-попап при клике по rad cursor.
            s.radPopupPropsC { RadPopupR.apply }

          )
        },


        <.br,

        // Рендерить табличку с данными по рассчёту текущей цены:
        s.priceDslOptC { ItemsPricesR.apply }

      )
    }

  }


  val component = ScalaComponent.builder[Props]("LamForm")
    .initialStateFromProps { p =>
      State(
        mmapC         = p.connect(_.mmap),
        radOptsC      = p.connect(identity),
        rcvrsC        = p.connect(_.rcvrs),
        priceDslOptC  = p.connect(_.price.respDslOpt),
        currentPotC   = p.connect(_.current),
        radPopupPropsC = p.connect { p =>
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

  def apply(mRootProxy: Props) = component(mRootProxy)

}
