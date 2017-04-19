package io.suggest.lk.adn.map.r.nm

import diode.react.ModelProxy
import io.suggest.lk.adn.map.m.MNodeMarkerS
import io.suggest.maps.u.MapIcons
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 11:49
  * Description: react-компонент маркера узла.
  */
object NodeMarkerR {

  type Props = ModelProxy[MNodeMarkerS]

  protected[this] case class State()


  class Backend($: BackendScope[Props, State]) {

    private val _pinIcon = MapIcons.pinMarkerIcon()



    def render(p: Props, s: State): ReactElement = {
      val props = p()

      // Маркер центра круга.
      ???
    }

  }


  val component = ReactComponentB[Props]("NodeMarker")
    .initialState_P { p =>
      State()
    }
    .renderBackend[Backend]
    .build

  def apply(nodeMarkerProxy: Props) = component(nodeMarkerProxy)

}
