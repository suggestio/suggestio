package io.suggest.lk.adv.geo.r.oms

import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.lk.adv.geo.m.SetOnMainScreen
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactEventI}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 16:50
  * Description: React-компонент тривиального чекбокса размещения на главном экране.
  */
object OnMainScreenR {

  case class PropsVal(value: Boolean)

  type Props = ModelProxy[PropsVal]

  protected class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на изменение галочки onMainScreen. */
    def onMainScreenChanged(e: ReactEventI): Callback = {
      val oms2 = e.target.checked
      $.props >>= { p =>
        p.dispatchCB( SetOnMainScreen(oms2) )
      }
    }

    def render(p: Props) = {
      <.label(
        ^.`class` := Css.CLICKABLE,

        <.input(
          ^.`type`    := "checkbox",
          ^.checked   := p().value,
          ^.onChange ==> onMainScreenChanged
        ),
        <.span(
          ^.`class` := Css.Input.STYLED_CHECKBOX
        ),
        Messages( "Adv.on.main.screen" )
      )
    }

  }

  val component = ReactComponentB[Props]("OnMainScreen")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}



