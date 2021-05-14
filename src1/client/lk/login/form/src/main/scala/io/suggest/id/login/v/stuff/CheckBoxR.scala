package io.suggest.id.login.v.stuff

import com.materialui.{MuiCheckBox, MuiCheckBoxClasses, MuiCheckBoxProps, MuiColorTypes}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.id.login.m.ICheckBoxActionStatic
import io.suggest.id.login.v.LoginFormCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, React, ReactEventFromInput, ScalaComponent}
import japgolly.univeq._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.19 18:03
  * Description: wrap() react-компонент галочки для формы логина/регистрации.
  */
class CheckBoxR(
                 loginFormCssCtx     : React.Context[LoginFormCss],
               ) {

  case class PropsVal(
                       checked    : Boolean,
                       onChange   : ICheckBoxActionStatic,
                       disabled   : Boolean                 = false,
                     )
  implicit object CheckBoxRFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.checked ==* b.checked) &&
      //(a.onChange ===* b.onChange) && // Не нужно для рендера - оно дёргается из callback.
      (a.disabled ==* b.disabled)
    }
  }

  type Props_t  = PropsVal
  type Props    = ModelProxy[Props_t]


  case class State(
                    propsC    : ReactConnectProxy[PropsVal],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onForeignPcChange( event: ReactEventFromInput, checked: Boolean ): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        propsProxy.value.onChange( checked )
      }
    }
    private val _onForeignPcChangeCbF = ReactCommonUtil.cbFun2ToJsCb( _onForeignPcChange )


    def render(p: Props, s: State): VdomElement = {
      loginFormCssCtx.consume { loginFormCss =>
        val cbCss = new MuiCheckBoxClasses {
          override val root = loginFormCss.formControl.htmlClass
        }
        s.propsC { propsProxy =>
          val props = propsProxy.value
          MuiCheckBox(
            new MuiCheckBoxProps {
              @JSName("onChange")
              override val onChange2 = _onForeignPcChangeCbF
              override val checked = js.defined( props.checked )
              override val classes = cbCss
              override val disabled = props.disabled
              override val color = MuiColorTypes.secondary
            }
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        propsC = propsProxy.connect( identity )( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
