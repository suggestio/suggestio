package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProps, ReactConnectProxy}
import io.suggest.lk.adn.map.m.IRadOpts
import io.suggest.maps.m.MRadT
import io.suggest.maps.m.MRadT.MRadTFastEq
import io.suggest.maps.r.rad.RadMapControlsR
import io.suggest.spa.OptFastEq.Wrapped
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 11:49
  * Description: react-компонент маркера узла.
  */
object MapCursorR {

  type Props_t = IRadOpts[_]
  type Props = ModelProxy[Props_t]


  protected[this] case class State(
                                    mRadTOptC      : ReactConnectProxy[Option[MRadT[_]]]
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      // Опциональное размещение в круге и в точке.
      s.mRadTOptC { RadMapControlsR.apply }
    }

  }


  val component = ScalaComponent.builder[Props]("NodeMarker")
    .initialStateFromProps { p =>
      State(
        mRadTOptC = p.connect { radOpts =>
          Some( radOpts.rad )
        }
      )
    }
    .renderBackend[Backend]
    .build

  private def _apply(nodeMarkerProxy: Props) = component(nodeMarkerProxy)
  val apply: ReactConnectProps[Props_t] = _apply

}
