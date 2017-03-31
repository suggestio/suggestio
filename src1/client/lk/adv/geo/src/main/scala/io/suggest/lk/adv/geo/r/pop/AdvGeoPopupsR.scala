package io.suggest.lk.adv.geo.r.pop

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adv.geo.m.{MNodeInfoPopupS, MPopupsS}
import io.suggest.lk.pop.PopupsContR
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import io.suggest.sjs.common.spa.OptFastEq.Wrapped
import PopupsContR.PopContPropsValFastEq
import MNodeInfoPopupS.MNodeInfoPopupFastEq

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
                              nodeInfoConn        : ReactConnectProxy[Option[MNodeInfoPopupS]]
                            )


  class Backend($: BackendScope[Props, State]) {

    def render(state: State): ReactElement = {
      state.popContPropsConn { popContPropsProxy =>
        // Рендер контейнера попапов:
        PopupsContR( popContPropsProxy )(

          // Попап инфы по размещению на узле.
          state.nodeInfoConn { AdvGeoNodeInfoPopR.apply }

        )
      }
    }

  }


  val component = ReactComponentB[Props]("AdvGeoPopups")
    .initialState_P { propsProxy =>
      State(
        popContPropsConn = propsProxy.connect { props =>
          // Храним строку css-классов снаружи функции, чтобы избежать ложных отрицательных результатов a.css eq b.css.
          PopupsContR.PropsVal(
            visible   = props.nonEmpty
          )
        },
        nodeInfoConn = propsProxy.connect( _.nodeInfo )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(mPopupsProxy: Props) = component( mPopupsProxy )

}
