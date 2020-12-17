package io.suggest.lk.adv.geo.r.oms

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.adv.geo.m.SetOnMainScreen
import io.suggest.msg.Messages
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 16:50
  * Description: React-компонент тривиального чекбокса размещения на главном экране.
  */
final class OnMainScreenR {

  case class PropsVal(value: Boolean)

  type Props = ModelProxy[PropsVal]

  protected class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на изменение галочки onMainScreen. */
    def onMainScreenChanged(e: ReactEventFromInput): Callback = {
      val oms2 = e.target.checked
      dispatchOnProxyScopeCB( $, SetOnMainScreen(oms2) )
    }

    def render(p: Props): VdomElement = {
      <.label(
        ^.`class` := Css.CLICKABLE,

        <.input(
          ^.`type`    := HtmlConstants.Input.checkbox,
          ^.checked   := p().value,
          ^.onChange ==> onMainScreenChanged
        ),

        <.span(
          ^.`class` := Css.Input.STYLED_CHECKBOX
        ),

        Messages( MsgCodes.`Adv.on.main.screen` )
      )
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}



