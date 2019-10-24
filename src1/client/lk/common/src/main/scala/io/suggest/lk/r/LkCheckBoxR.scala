package io.suggest.lk.r

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.css.Css
import io.suggest.react.ReactDiodeUtil
import io.suggest.spa.{DAction, OptFastEq}
import japgolly.scalajs.react._
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
                       label      : VdomNode,
                       checked    : Boolean,
                       onChange   : Boolean => DAction,
                     )
  /** FastEq без проверки инстанса onChange: */
  object LkCheckBoxMinimalFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.label eq b.label) &&
      (a.checked ==* b.checked)
    }
  }
  implicit object LkCheckBoxRFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      LkCheckBoxMinimalFastEq.eqv(a, b) &&
      (a.onChange eq b.onChange)
    }
  }

  case class State(
                    isVisible       : ReactConnectProxy[Some[Boolean]],
                    checkedSomeC    : ReactConnectProxy[Option[Boolean]],
                    labelC          : ReactConnectProxy[VdomNode],
                  )

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, State]) {

    private def onCheckboxChange(e: ReactEventFromInput): Callback = {
      val isEnabled = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        propsProxy.value
          .get    // TODO Callback.empty
          .onChange(isEnabled)
      }
    }

    def render(s: State): VdomElement = {
      <.label(
        ^.`class` := Css.CLICKABLE,

        s.checkedSomeC { isCheckedOptProxy =>
          isCheckedOptProxy.value.whenDefinedEl { isChecked =>
            <.input(
              ^.`type`    := HtmlConstants.Input.checkbox,
              ^.checked   := isChecked,
              ^.onChange ==> onCheckboxChange
            )
          }
        },

        <.span(
          ^.`class` := Css.Input.STYLED_CHECKBOX
        ),

        s.labelC { labelProxy =>
          <.span(
            ^.`class` := Css.flat( Css.Input.CHECKBOX_TITLE, Css.Buttons.MAJOR ),
            labelProxy.value
          )
        },
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsOptProxy =>
      State(
        isVisible = propsOptProxy.connect { propsOpt =>
          OptionUtil.SomeBool( propsOpt.nonEmpty )
        }( FastEq.AnyRefEq ),
        checkedSomeC = propsOptProxy.connect { propsOpt =>
          for (p <- propsOpt)
          yield p.checked
        }( OptFastEq.OptValueEq ),
        labelC = propsOptProxy.connect( _.fold(EmptyVdom)(_.label) )( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply( propsProxy: Props ) = component( propsProxy )

}
