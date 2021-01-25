package io.suggest.lk.adv.geo.r.pop

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adv.geo.m.{MNodeInfoPopupS, MPopupsS}
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import io.suggest.spa.OptFastEq.Wrapped
import MNodeInfoPopupS.MNodeInfoPopupFastEq
import io.suggest.lk.m.MErrorPopupS
import io.suggest.lk.r.{ErrorPopupR, PleaseWaitPopupR}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import MErrorPopupS.MErrorPopupSFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 16:13
  * Description: React-компонент попапов.
  */
final class AdvGeoPopupsR(
                           errorPopupR: ErrorPopupR,
                           advGeoNodeInfoPopR: AdvGeoNodeInfoPopR,
                         ) {

  type Props = ModelProxy[MPopupsS]

  protected case class State(
                              nodeInfoConn        : ReactConnectProxy[Option[MNodeInfoPopupS]],
                              errorOptConn        : ReactConnectProxy[Option[MErrorPopupS]]
                            )


  class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, state: State): VdomElement = {
      React.Fragment(

        // Попап "Пожалуйста, подождите...":
        propsProxy.wrap(_.firstPotPending)( PleaseWaitPopupR.component.apply ),

        // Попап с какой-либо ошибкой среди попапов.
        state.errorOptConn { errorPopupR.component.apply },

        // Попап инфы по размещению на узле.
        state.nodeInfoConn { advGeoNodeInfoPopR.component.apply },

      )

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        nodeInfoConn    = propsProxy.connect( _.nodeInfo ),
        errorOptConn    = propsProxy.connect { props =>
          MErrorPopupS.fromExOpt(
            props.firstPotFailed
          )
        }
      )
    }
    .renderBackend[Backend]
    .build

}
