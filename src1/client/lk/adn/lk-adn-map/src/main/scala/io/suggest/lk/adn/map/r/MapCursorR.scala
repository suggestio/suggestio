package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.maps.m.MRad
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

  type Props_t = Option[MRad]
  type Props = ModelProxy[Props_t]


  protected[this] case class State(
                                    mRadOptC      : ReactConnectProxy[Option[MRad]],
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      // Опциональное размещение в круге и в точке.
      s.mRadOptC { RadMapControlsR.component.apply }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { p =>
      State(
        mRadOptC = p.connect( identity ),
      )
    }
    .renderBackend[Backend]
    .build

}
