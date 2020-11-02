package io.suggest.id.login.v.reg

import com.materialui.{Mui, MuiColorTypes, MuiFormGroup, MuiFormGroupProps, MuiIconButton, MuiIconButtonProps, MuiSnackBarContent, MuiSnackBarContentProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.IdentConst
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.{PwReset, RegEmailBlur, RegEmailEdit, RegPhoneBlur, RegPhoneEdit}
import io.suggest.id.login.v.stuff.{ErrorSnackR, TextFieldR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.DiodeUtil.Implicits._
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
                  errorSnackR     : ErrorSnackR,
                  crCtxProv       : React.Context[MCommonReactCtx],
                ) {

  type Props = ModelProxy[MReg0Creds]


  case class State(
                    pwRecoverMsgShownSomeC      : ReactConnectProxy[Some[Boolean]],
                    submitReqExC                : ReactConnectProxy[Throwable],
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
          lazy val msgSnack = {
            val _msg = crCtxProv.message( MsgCodes.`Type.previous.signup.data.to.start.password.recovery` )

            val _close = MuiIconButton(
              new MuiIconButtonProps {
                override val color = MuiColorTypes.inherit
                override val onClick = _onClosePwResetMsgClickCbF
              }
            )(
              Mui.SvgIcons.Close()()
            )
            MuiSnackBarContent(
              new MuiSnackBarContentProps {
                override val action  = _close.rawNode
                override val message = _msg.rawNode
              }
            )
          }
          s.pwRecoverMsgShownSomeC { pwRecoverMsgShownSomeProxy =>
            ReactCommonUtil.maybeEl( pwRecoverMsgShownSomeProxy.value.value )( msgSnack )
          }
        },

        // Поле ввода номера телефона:
        {
          val regPhoneBlurSome = Some( RegPhoneBlur )
          val mkActionSome = Some( RegPhoneEdit )
          p.wrap { props =>
            textFieldR.PropsVal(
              state       = props.phone,
              hasError    = false,
              mkAction    = mkActionSome,
              inputType   = HtmlConstants.Input.tel,
              autoFocus   = true,
              inputName   = IdentConst.Login.PHONE_FN,
              label       = MsgCodes.`Mobile.phone.number`,
              placeHolder = MsgCodes.`Phone.number.example`,
              onBlur      = regPhoneBlurSome,
            )
          }(textFieldR.apply)(implicitly, textFieldR.EpwTextFieldPropsValFastEq)
        },

        // Поле ввода email:
        {
          val regEmailBlurSome = Some( RegEmailBlur )
          val mkActionSome = Some( RegEmailEdit )
          p.wrap { props =>
            textFieldR.PropsVal(
              state       = props.email,
              hasError    = false,
              mkAction    = mkActionSome,
              inputType   = HtmlConstants.Input.email,
              autoFocus   = false,
              inputName   = IdentConst.Login.NAME_FN,
              label       = MsgCodes.`Your.email.addr`,
              onBlur      = regEmailBlurSome,
              placeHolder = MsgCodes.`Email.example`,
            )
          }(textFieldR.apply)(implicitly, textFieldR.EpwTextFieldPropsValFastEq)
        },

        // Рендер ошибки запроса токена нулевого шага:
        s.submitReqExC { errorSnackR.component.apply },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        pwRecoverMsgShownSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.pwRecoverMsg )
        }( FastEq.AnyRefEq ),

        submitReqExC = propsProxy.connect( _.submitReq.exceptionOrNull )( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

}
