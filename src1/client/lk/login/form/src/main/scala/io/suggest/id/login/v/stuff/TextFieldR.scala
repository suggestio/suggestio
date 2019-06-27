package io.suggest.id.login.v.stuff

import chandu0101.scalajs.react.components.materialui.{MuiFormControlClasses, MuiTextField, MuiTextFieldProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MCommonReactCtx
import io.suggest.id.login.v.LoginFormCss
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.react.ReactCommonUtil
import io.suggest.spa.DAction
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.univeq._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.19 14:20
  * Description: Компонент для текстового поля имени или пароля.
  */
class TextFieldR(
                  commonReactCtxProv          : React.Context[MCommonReactCtx],
                  loginFormCssCtx             : React.Context[LoginFormCss],
                ) {


  case class PropsVal(
                       state        : MTextFieldS,
                       hasError     : Boolean,
                       mkAction     : Option[String => DAction],
                       isPassword   : Boolean,
                       inputName    : String,
                       label        : String,
                       placeHolder  : String,
                       onBlur       : Option[DAction] = None,
                       disabled     : Boolean = false,
                       required     : Boolean = true,
                     )
  implicit object EpwTextFieldPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.state          ===* b.state) &&
      (a.disabled        ==* b.disabled) &&
      (a.hasError        ==* b.hasError) &&
      (a.mkAction         eq b.mkAction) &&
      (a.isPassword      ==* b.isPassword) &&
      (a.inputName      ===* b.inputName) &&
      (a.label          ===* b.label) &&
      (a.placeHolder    ===* b.placeHolder) &&
      (a.onBlur         ===* b.onBlur) &&
      (a.required        ==* b.required)
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
      $.props >>= { p: Props =>
        p.value.mkAction
          .fold [Callback]( Callback.empty ) { f => p.dispatchCB(f(value2)) }
      }
    }
    private lazy val _onFieldChangeCbF = ReactCommonUtil.cbFun1ToJsCb( _onFieldChange )


    private def _onFieldBlur(event: ReactFocusEvent): Callback = {
      $.props >>= { p: Props =>
        p.value.onBlur
          .fold( Callback.empty )( p.dispatchCB )
      }
    }
    private lazy val _onFieldBlurCbF = ReactCommonUtil.cbFun1ToJsCb( _onFieldBlur )


    def render(s: State): VdomElement = {
      loginFormCssCtx.consume { loginFormCss =>
        val mfcCss = new MuiFormControlClasses {
          override val root = loginFormCss.formControl.htmlClass
        }
        commonReactCtxProv.consume { crCtx =>
          s.propsValC { propsProxy =>
            val p = propsProxy.value
            val _onBlurUndef   = JsOptionUtil.maybeDefined( p.onBlur.nonEmpty )( _onFieldBlurCbF )
            val _onChangeUndef = JsOptionUtil.maybeDefined( p.mkAction.nonEmpty )( _onFieldChangeCbF )
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
                override val onChange     = _onChangeUndef
                override val label        = crCtx.messages( p.label )
                override val placeholder  = crCtx.messages( p.placeHolder )
                override val required     = p.required
                override val autoFocus    = !p.isPassword
                override val fullWidth    = true
                override val classes      = mfcCss
                override val error        = p.hasError || !p.state.isValid
                override val onBlur       = _onBlurUndef
                override val disabled     = p.disabled
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
