package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adn.map.m.IRadOpts
import io.suggest.maps.m.MRadT
import io.suggest.maps.m.MRadT.MRadTFastEq
import io.suggest.maps.r.rad.RadMapControlsR
import io.suggest.sjs.common.spa.OptFastEq.Wrapped
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 11:49
  * Description: react-компонент маркера узла.
  */
object MapCursorR {

  type Props = ModelProxy[IRadOpts[_]]


  protected[this] case class State(
                                    mRadTOptC      : ReactConnectProxy[Option[MRadT[_]]]
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): ReactElement = {
      // Опциональное размещение в круге и в точке.
      s.mRadTOptC { RadMapControlsR.apply }
    }

  }


  val component = ReactComponentB[Props]("NodeMarker")
    .initialState_P { p =>
      State(
        mRadTOptC = p.connect { radOpts =>
          Some( radOpts.rad )
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(nodeMarkerProxy: Props) = component(nodeMarkerProxy)

}
