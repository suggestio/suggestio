package io.suggest.id.login.v.epw

import chandu0101.scalajs.react.components.materialui.{MuiFormControlClasses, MuiTextField, MuiTextFieldProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MCommonReactCtx
import io.suggest.id.login.m._
import io.suggest.id.login.m.epw.MEpwTextFieldS
import io.suggest.id.login.v.LoginFormCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEventFromInput, ScalaComponent}
import japgolly.univeq._

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.19 14:20
  * Description: Компонент для текстового поля имени или пароля.
  */
class EpwTextFieldR(
                     commonReactCtxProv          : React.Context[MCommonReactCtx],
                     loginFormCssCtx             : React.Context[LoginFormCss],
                   ) {


  case class PropsVal(
                       state        : MEpwTextFieldS,
                       hasError      : Boolean,
                       mkAction     : IEpwSetValueStatic,
                       isPassword   : Boolean,
                       inputName    : String,
                       msgCode      : String,
                     )
  implicit object EpwTextFieldPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.state ===* b.state) &&
      (a.hasError ==* b.hasError) &&
      (a.mkAction ===* b.mkAction) &&
      (a.isPassword ==* b.isPassword) &&
      (a.inputName ===* b.inputName) &&
      (a.msgCode ===* b.msgCode)
    }
  }

  case class State(
                    propsValC       : ReactConnectProxy[PropsVal],
                  )

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, State]) {

    private def _onFieldChange(event: ReactEventFromInput): Callback = {
      val value2 = event.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { props: Props =>
        props.value.mkAction( value2 )
      }
    }
    private val _onFieldChangeCbF = ReactCommonUtil.cbFun1ToJsCb( _onFieldChange )

    def render(propsProxy: Props, s: State): VdomElement = {
      loginFormCssCtx.consume { loginFormCss =>
        val mfcCss = new MuiFormControlClasses {
          override val root = loginFormCss.epwFormControl.htmlClass
        }
        commonReactCtxProv.consume { crCtx =>
          s.propsValC { propsProxy =>
            val p = propsProxy.value
            MuiTextField(
              new MuiTextFieldProps {
                override val value = js.defined {
                  p.state.value
                }
                override val `type` = {
                  if (p.isPassword) HtmlConstants.Input.password
                  else HtmlConstants.Input.text
                }
                override val name         = p.inputName
                override val onChange     = _onFieldChangeCbF
                override val placeholder  = crCtx.messages( p.msgCode )
                override val required     = true
                override val autoFocus    = !p.isPassword
                override val fullWidth    = true
                override val classes      = mfcCss
                override val error        = p.hasError
              }
            )
          }
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsValProxy =>
      State(
        propsValC = propsValProxy.connect(identity)( EpwTextFieldPropsValFastEq )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsValProxy: Props) = component(propsValProxy)

}
