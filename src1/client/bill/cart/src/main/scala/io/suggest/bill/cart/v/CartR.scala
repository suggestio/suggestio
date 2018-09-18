package io.suggest.bill.cart.v

import diode.react.ModelProxy
import io.suggest.bill.cart.m.MCartRootS
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 18:20
  * Description: Корневой компонент корзины приобретённых товаров и услуг.
  */
class CartR {

  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]

  case class State(
                  )

  /** Ядро компонента корзины. */
  class Backend( $: BackendScope[Props, State] ) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(
        "TODO"
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
