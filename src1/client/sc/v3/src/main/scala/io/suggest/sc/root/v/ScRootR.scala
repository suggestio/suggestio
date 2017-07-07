package io.suggest.sc.root.v

import diode.react.ModelProxy
import io.suggest.sc.ScCss
import io.suggest.sc.root.m.MScRoot
import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 14:06
  * Description: Корневой react-компонент для выдачи третьего поколения.
  */
object ScRootR {

  type Props = ModelProxy[MScRoot]

  //protected[this] case class State()
  type State = Unit

  class Backend($: BackendScope[Props, State]) {

    def render(p: Props): VdomElement = {
      <.div(
        ScCss.Root.root,

        // TODO Header component.
        // TODO Focused
        // TODO Grid
      )
    }

  }

}
