package io.suggest.id.login.v.stuff

import com.materialui.{Mui, MuiButtonSizes, MuiFormControlClasses, MuiIconButton, MuiIconButtonProps, MuiInputAdornment, MuiInputAdornmentPositions, MuiInputAdornmentProps, MuiInputProps, MuiTextField, MuiTextFieldProps}
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
import JsOptionUtil.Implicits._

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
                       visibilityChange : Option[Boolean => DAction] = None,
                     )
  implicit object EpwTextFieldPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.state          ===* b.state) &&
      (a.disabled        ==* b.disabled) &&
      (a.hasError        ==* b.hasError) &&
      (a.mkAction.isEmpty ==* b.mkAction.isEmpty) &&
      (a.isPassword      ==* b.isPassword) &&
      (a.inputName      ===* b.inputName) &&
      (a.label          ===* b.label) &&
      (a.placeHolder    ===* b.placeHolder) &&
      (a.onBlur         ===* b.onBlur) &&
      (a.required        ==* b.required) &&
      (a.visibilityChange.isEmpty ==* b.visibilityChange.isEmpty)
    }
  }

  case class State(
                    propsValC       : ReactConnectProxy[PropsVal],
                  )

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, State]) {

    private lazy val _onVisibilityClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      $.props >>= { propsProxy: Props =>
        val p = propsProxy.value
        p.visibilityChange
          .fold[Callback]( Callback.empty ) { visibilityChangeF =>
            val isVisibleNext = p.isPassword
            propsProxy.dispatchCB( visibilityChangeF(isVisibleNext) )
          }
      }
    }

    private lazy val _onFieldChangeCbF = ReactCommonUtil.cbFun1ToJsCb { event: ReactEventFromInput =>
      val value2 = event.target.value
      $.props >>= { p: Props =>
        p.value.mkAction
          .fold [Callback]( Callback.empty ) { f => p.dispatchCB(f(value2)) }
      }
    }

    private lazy val _onFieldBlurCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactFocusEvent =>
      $.props >>= { p: Props =>
        p.value.onBlur
          .fold( Callback.empty )( p.dispatchCB )
      }
    }


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

            // Добавить InputAdornment, если допускается возможность переключения видимости (для пароля).
            val inpAdornProps = for {
              _ <- p.visibilityChange
            } yield {
              val adorn = MuiInputAdornment(
                new MuiInputAdornmentProps {
                  override val position = MuiInputAdornmentPositions.end
                }
              )(
                // Инонка visibility on/off.
                MuiIconButton(
                  new MuiIconButtonProps {
                    override val onClick = _onVisibilityClick
                    override val size = MuiButtonSizes.small
                  }
                )(
                  (if (p.isPassword)
                    Mui.SvgIcons.Visibility
                  else
                    Mui.SvgIcons.VisibilityOff)()(),
                ),
              )

              new MuiInputProps {
                override val endAdornment = adorn.rawNode
              }
            }

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
                // для возможности управления visibility пароля, пробрасываем дополнительные input props:
                override val InputProps   = inpAdornProps.toUndef
              }
            )()
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
