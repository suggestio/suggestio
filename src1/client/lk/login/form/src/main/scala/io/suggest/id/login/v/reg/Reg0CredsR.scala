package io.suggest.id.login.v.reg

import com.materialui.{Mui, MuiColorTypes, MuiFormGroup, MuiFormGroupProps, MuiIconButton, MuiIconButtonProps, MuiSnackBar, MuiSnackBarContent, MuiSnackBarContentProps, MuiSnackBarProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.IdentConst
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.{PwReset, RegEmailBlur, RegEmailEdit, RegPhoneBlur, RegPhoneEdit}
import io.suggest.id.login.v.stuff.TextFieldR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 17:29
  * Description: Страница регистрации с вводом необходимых реквизитов.
  */
class Reg0CredsR(
                  textFieldR      : TextFieldR,
                  commonReactCtxP : React.Context[MCommonReactCtx],
                ) {

  type Props = ModelProxy[MReg0Creds]


  case class State(
                    pwRecoverMsgShownSome       : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend( $: BackendScope[Props, State] ) {


    private def _onClosePwResetMsgClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, PwReset(enable = false) )
    private lazy val _onClosePwResetMsgClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onClosePwResetMsgClick )


    def render(p: Props, s: State): VdomElement = {
      MuiFormGroup(
        new MuiFormGroupProps {
          override val row = true
        }
      )(

        // Подсказка о восстановлении пароля через регистрацию.
        {
          val _msg = commonReactCtxP.consume { crCtx =>
            crCtx.messages( MsgCodes.`Type.previous.signup.data.to.start.password.recovery` )
          }
          val _close = MuiIconButton(
            new MuiIconButtonProps {
              override val color = MuiColorTypes.inherit
              override val onClick = _onClosePwResetMsgClickCbF
            }
          )(
            Mui.SvgIcons.Close()()
          )
          val msgSnack = MuiSnackBarContent(
            new MuiSnackBarContentProps {
              override val action  = _close.rawNode
              override val message = _msg.rawNode
            }
          )
          s.pwRecoverMsgShownSome { pwRecoverMsgShownSomeProxy =>
            MuiSnackBar(
              new MuiSnackBarProps {
                override val open = pwRecoverMsgShownSomeProxy.value.value
              }
            )(
              msgSnack
            )
          }
        },

        // Поле ввода email:
        {
          val regEmailBlurSome = Some( RegEmailBlur )
          val mkActionSome = Some( RegEmailEdit.apply _ )
          p.wrap { props =>
            textFieldR.PropsVal(
              state       = props.email,
              hasError    = false,
              mkAction    = mkActionSome,
              isPassword  = false,
              inputName   = IdentConst.Login.NAME_FN,
              label       = MsgCodes.`Your.email.addr`,
              onBlur      = regEmailBlurSome,
              placeHolder = MsgCodes.`Email.example`,
            )
          }(textFieldR.apply)(implicitly, textFieldR.EpwTextFieldPropsValFastEq)
        },


        // Поле ввода номера телефона:
        {
          val regPhoneBlurSome = Some( RegPhoneBlur )
          val mkActionSome = Some( RegPhoneEdit.apply _ )
          p.wrap { props =>
            textFieldR.PropsVal(
              state       = props.phone,
              hasError    = false,
              mkAction    = mkActionSome,
              isPassword  = false,
              inputName   = IdentConst.Login.PHONE_FN,
              label       = MsgCodes.`Mobile.phone.number`,
              placeHolder = MsgCodes.`Phone.number.example`,
              onBlur      = regPhoneBlurSome,
            )
          }(textFieldR.apply)(implicitly, textFieldR.EpwTextFieldPropsValFastEq)
        },
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        pwRecoverMsgShownSome = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.pwRecoverMsg )
        }( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

}
