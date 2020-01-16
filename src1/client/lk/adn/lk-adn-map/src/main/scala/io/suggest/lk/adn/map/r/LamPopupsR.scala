package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.lk.adn.map.m.MRoot
import io.suggest.lk.m.MErrorPopupS
import io.suggest.lk.r.ErrorPopupR
import io.suggest.lk.r.adv.NodeAdvInfoPopR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import io.suggest.spa.OptFastEq.Plain
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import japgolly.scalajs.react.vdom.Implicits._
import MErrorPopupS.MErrorPopupSFastEq
import io.suggest.lk.r.popup.PopupsContR
import io.suggest.spa.OptFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.06.17 16:24
  * Description: Компонент обычных html-попапов для Lk-adn-map-формы.
  */
object LamPopupsR {

  type Props = ModelProxy[MRoot]

  protected case class State(
                              popContPropsC             : ReactConnectProxy[PopupsContR.PropsVal],
                              nodeAdvInfoOptC           : ReactConnectProxy[Option[MNodeAdvInfo]],
                              errorPopupOptC            : ReactConnectProxy[Option[MErrorPopupS]]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    def render(state: State): VdomElement = {

      val popups = Seq[VdomNode](
        // Попап инфы по размещению на узле.
        state.nodeAdvInfoOptC { NodeAdvInfoPopR.apply },

        // Попап с какой-либо ошибкой среди попапов.
        state.errorPopupOptC { ErrorPopupR.apply }
      )

      state.popContPropsC { popContPropsProxy =>
        // Рендер контейнера попапов:
        PopupsContR( popContPropsProxy )(
          popups: _*
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("LamPops")
    .initialStateFromProps { mrootProxy =>
      State(
        popContPropsC = mrootProxy.connect { mroot =>
          // Храним строку css-классов снаружи функции, чтобы избежать ложных отрицательных результатов a.css eq b.css.
          val prPot = mroot.rcvrs.popupResp
          PopupsContR.PropsVal(
            visible = prPot.nonEmpty || prPot.isFailed
          )
        },
        nodeAdvInfoOptC = mrootProxy.connect { _.rcvrs.popupResp.toOption },
        errorPopupOptC = mrootProxy.connect { mroot =>
          MErrorPopupS.fromExOpt(
            mroot.rcvrs.popupResp.exceptionOption
          )
        }( OptFastEq.Wrapped )
      )
    }
    .renderBackend[Backend]
    .build


  def apply(mroot: Props) = component(mroot)

}
