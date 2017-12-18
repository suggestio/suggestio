package io.suggest.ad.edit.v.edit.strip

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.StripStretchAcross
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.17 10:57
  * Description: React-компонента галочки широкоформатного отображения.
  */
class ShowWideR {

  /** Модель пропертисов данного компонента.
    *
    * @param checked Текущее состояние галочки широкого отображения.
    */
  case class PropsVal(
                       checked: Boolean
                     )
  implicit object ShowWideRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.checked ==* b.checked
    }
  }


  type Props = ModelProxy[Option[PropsVal]]

  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на изменение состояния галочки широкоформатного отображения. */
    private def _onCheckedChange(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      dispatchOnProxyScopeCB($, StripStretchAcross(isChecked))
    }

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        <.div(
          <.label(
            ^.`class` := Css.CLICKABLE,

            <.input(
              ^.`type`    := HtmlConstants.Input.checkbox,
              ^.checked   := props.checked,
              ^.onChange ==> _onCheckedChange
            ),

            <.span(
              ^.`class` := Css.Input.STYLED_CHECKBOX
            ),

            <.span(
              ^.`class` := Css.flat(Css.Input.CHECKBOX_TITLE, Css.Buttons.MAJOR),
              Messages( MsgCodes.`Stretch.across` )
            )
          )
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("ShowWide")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component(propsOptProxy)

}
