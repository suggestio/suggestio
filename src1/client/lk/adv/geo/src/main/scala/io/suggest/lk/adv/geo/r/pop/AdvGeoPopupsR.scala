package io.suggest.lk.adv.geo.r.pop

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adv.geo.m.{MNodeInfoPopupS, MPopupsS}
import io.suggest.lk.pop.PopupsContR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import io.suggest.spa.OptFastEq.Wrapped
import PopupsContR.PopContPropsValFastEq
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
object AdvGeoPopupsR {

  type Props = ModelProxy[MPopupsS]

  protected case class State(
                              popContPropsConn    : ReactConnectProxy[PopupsContR.PropsVal],
                              nodeInfoConn        : ReactConnectProxy[Option[MNodeInfoPopupS]],
                              pendingOptConn      : ReactConnectProxy[Option[Long]],
                              errorOptConn        : ReactConnectProxy[Option[MErrorPopupS]]
                            )


  class Backend($: BackendScope[Props, State]) {

    def render(state: State): VdomElement = {
      state.popContPropsConn { popContPropsProxy =>
        // Рендер контейнера попапов:
        PopupsContR( popContPropsProxy )(

          // Попап инфы по размещению на узле.
          state.nodeInfoConn { AdvGeoNodeInfoPopR.apply },

          // -- Служебные попапы --
          // Попап "Пожалуйста, подождите...":
          state.pendingOptConn { PleaseWaitPopupR.apply },

          // Попап с какой-либо ошибкой среди попапов.
          state.errorOptConn { ErrorPopupR.apply }

        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("AdvGeoPopups")
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

  def apply(mPopupsProxy: Props) = component( mPopupsProxy )

}
