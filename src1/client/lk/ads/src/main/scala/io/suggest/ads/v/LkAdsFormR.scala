package io.suggest.ads.v

import diode.react.ModelProxy
import io.suggest.ads.m.MLkAdsRoot
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 22:05
  * Description: Корневой react-компонент формы управления карточками узла.
  * Дабы избежать лишних проблем, все коннекшены живут здесь и все под-компоненты дёргаются только отсюда.
  */
class LkAdsFormR {

  type Props = ModelProxy[MLkAdsRoot]

  case class State()


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.div(
        "TODO"
      )
    }

  }


  val component = ScalaComponent.builder[Props]("LkAdsForm")
    .initialStateFromProps { propsProxy =>
      State()
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
