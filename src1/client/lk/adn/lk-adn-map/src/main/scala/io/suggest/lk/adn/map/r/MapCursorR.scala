package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adn.map.m.MLamRad
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
final class MapCursorR {

  type Props_t = MLamRad
  type Props = ModelProxy[Props_t]


  protected[this] case class State(
                                    mRadC      : ReactConnectProxy[MLamRad],
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      // Опциональное размещение в круге и в точке.
      s.mRadC { mradProxy =>
        RadMapControlsR.component( mradProxy.zoom(Some.apply) )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { p =>
      State(
        mRadC = p.connect( identity ),
      )
    }
    .renderBackend[Backend]
    .build

}
