package io.suggest.lk.adv.geo.r.pop

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adv.geo.m.{MNodeInfoPopupS, MPopupsS}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import io.suggest.spa.OptFastEq.Wrapped
import MNodeInfoPopupS.MNodeInfoPopupFastEq
import io.suggest.lk.m.MErrorPopupS
import io.suggest.lk.r.{ErrorPopupR, PleaseWaitPopupR}
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import japgolly.scalajs.react.vdom.Implicits._
import MErrorPopupS.MErrorPopupSFastEq
import io.suggest.lk.r.popup.PopupsContR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 16:13
  * Description: React-компонент попапов.
  */
final class AdvGeoPopupsR(
                           advGeoNodeInfoPopR: AdvGeoNodeInfoPopR
                         ) {

  type Props = ModelProxy[MPopupsS]

  protected case class State(
                              popContPropsConn    : ReactConnectProxy[PopupsContR.PropsVal],
                              nodeInfoConn        : ReactConnectProxy[Option[MNodeInfoPopupS]],
                              pendingOptConn      : ReactConnectProxy[Option[Long]],
                              errorOptConn        : ReactConnectProxy[Option[MErrorPopupS]]
                            )


  class Backend($: BackendScope[Props, State]) {

    def render(state: State): VdomElement = {
      val popupsChildren = List[VdomNode](
        // Попап инфы по размещению на узле.
        state.nodeInfoConn { advGeoNodeInfoPopR.component.apply },

        // Попап "Пожалуйста, подождите...":
        state.pendingOptConn { PleaseWaitPopupR.apply },

        // Попап с какой-либо ошибкой среди попапов.
        state.errorOptConn { ErrorPopupR.component.apply }

      )

      state.popContPropsConn { popContPropsProxy =>
        // Рендер контейнера попапов:
        PopupsContR( popContPropsProxy )(
          popupsChildren: _*
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        popContPropsConn = propsProxy.connect { props =>
          // Храним строку css-классов снаружи функции, чтобы избежать ложных отрицательных результатов a.css eq b.css.
          PopupsContR.PropsVal(
            visible = props.nonEmpty
          )
        },
        nodeInfoConn    = propsProxy.connect( _.nodeInfo ),
        pendingOptConn  = propsProxy.connect( _.firstPotPending ),
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
