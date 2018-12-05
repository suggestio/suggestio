package io.suggest.lk.r

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.react.ReactDiodeUtil
import io.suggest.spa.DAction
import japgolly.scalajs.react._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.18 23:13
  * Description: Чек-бокс в личном кабинете.
  * Не используем MuiCheckBox, т.к. выглядит страшновато местами в текущем дизайне.
  */
class LkCheckBoxR {

  case class PropsVal(
                       label      : String,
                       checked    : Boolean,
                       onChange   : Boolean => DAction,
                     )
  implicit object LkCheckBoxRFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.label ===* b.label) &&
      (a.checked ==* b.checked) &&
      (a.onChange eq b.onChange)
    }
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    private def onCheckboxChange(e: ReactEventFromInput): Callback = {
      val isEnabled = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        propsProxy.value.onChange(isEnabled)
      }
    }

    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value

      <.label(
        ^.`class` := Css.CLICKABLE,

        <.input(
          ^.`type` := HtmlConstants.Input.checkbox,
          ^.checked := props.checked,
          ^.onChange ==> onCheckboxChange
        ),

        <.span(
          ^.`class` := Css.Input.STYLED_CHECKBOX
        ),

        <.span(
          ^.`class` := Css.flat( Css.Input.CHECKBOX_TITLE, Css.Buttons.MAJOR ),
          props.label
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsProxy: Props ) = component( propsProxy )

}
