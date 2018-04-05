package io.suggest.adn.edit.v

import diode.react.ModelProxy
import io.suggest.adn.edit.m.MLkAdnEditRoot
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 19:33
  * Description: Корневой react-компонент формы редактирования ADN-узла.
  */
class LkAdnEditFormR {

  type Props = ModelProxy[MLkAdnEditRoot]

  case class State()

  class Backend($: BackendScope[Props, State]) {
    def render(props: Props, s: State): VdomElement = {
      <.div(
        "IT WORKS!"
      )
    }
  }

  val component = ScalaComponent.builder[Props]("Form")
    .initialStateFromProps { propsProxy =>
      State(
      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProps: Props) = component(rootProps)

}
