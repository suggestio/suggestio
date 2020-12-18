package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.lk.adn.map.m.MRoot
import io.suggest.lk.m.MErrorPopupS
import io.suggest.lk.r.ErrorPopupR
import io.suggest.lk.r.adv.NodeAdvInfoPopR
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import io.suggest.spa.OptFastEq.Plain
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import diode.data.Pot
import io.suggest.spa.{FastEqUtil, OptFastEq}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.06.17 16:24
  * Description: Компонент обычных html-попапов для Lk-adn-map-формы.
  */
final class LamPopupsR(
                        errorPopupR: ErrorPopupR,
                      ) {

  type Props = ModelProxy[MRoot]

  protected case class State(
                              nodeAdvInfoPotC           : ReactConnectProxy[Pot[MNodeAdvInfo]],
                              errorPopupOptC            : ReactConnectProxy[Option[Throwable]]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    def render(state: State): VdomElement = {
      React.Fragment(

        // Попап с какой-либо ошибкой среди попапов.
        state.errorPopupOptC { exOptProxy =>
          exOptProxy.wrap( MErrorPopupS.fromExOpt )( errorPopupR.component.apply )
        },

        state.nodeAdvInfoPotC { nodeAdvInfoPotProxy =>
          nodeAdvInfoPotProxy.wrap( _.toOption )( NodeAdvInfoPopR.component.apply )
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mrootProxy =>
      State(
        nodeAdvInfoPotC = mrootProxy.connect( _.rcvrs.popupResp )( FastEqUtil.PotAsOptionFastEq(FastEqUtil.AnyRefFastEq) ),
        errorPopupOptC = mrootProxy.connect { mroot =>
          mroot.rcvrs.popupResp.exceptionOption
        }( OptFastEq.Wrapped(FastEqUtil.AnyRefFastEq) ),
      )
    }
    .renderBackend[Backend]
    .build

}
