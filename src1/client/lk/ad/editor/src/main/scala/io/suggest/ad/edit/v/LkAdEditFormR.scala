package io.suggest.ad.edit.v

import diode.react.ModelProxy
import io.suggest.ad.edit.m.MAdEditRoot
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 21:56
  * Description: React-компонент всей формы react-редактора карточек.
  */
object LkAdEditFormR {

  type Props = ModelProxy[MAdEditRoot]

  /** Состояние компонента содержит model-коннекшены для подчинённых компонентов. */
  protected case class State()

  protected class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.div(
        "Hello, world!"
      )
    }

  }


  val component = ScalaComponent.builder[Props]("AdEd")
    .initialStateFromProps { p =>
      State()
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
