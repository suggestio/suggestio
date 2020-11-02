package io.suggest.id.login.v.reg

import com.materialui.{MuiFormGroup, MuiFormGroupProps, MuiFormLabel}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.reg.MRegS
import io.suggest.id.login.v.stuff.ErrorSnackR
import io.suggest.lk.r.sms.SmsCodeFormR
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.spa.DiodeUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 18:35
  * Description: Компонент формы ввода смс-кода.
  */
class Reg2SmsCodeR(
                    smsCodeFormR        : SmsCodeFormR,
                    errorSnackR         : ErrorSnackR,
                    commonReactCtxP     : React.Context[MCommonReactCtx],
                  ) {

  type Props = ModelProxy[MRegS]

  case class State(
                    phoneNumberC      : ReactConnectProxy[String],
                    submitReqExC      : ReactConnectProxy[Throwable],
                  )


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      MuiFormGroup(
        new MuiFormGroupProps {
          override val row = true
        }
      )(

        // Вывести сообщение, что отправлено смс на ранее указанный номер.
        MuiFormLabel()(
          commonReactCtxP.consume { commonCtx =>
            s.phoneNumberC { phoneNumberProxy =>
              val phoneNbsp = phoneNumberProxy.value
              <.span(
                commonCtx.messages( MsgCodes.`Sms.code.sent.to.your.phone.number.0`, phoneNbsp )
              )
            }
          }
        ),

        // Поле для ввода смс-кода:
        p.wrap { props =>
          for (smsCode <- props.s2SmsCode.smsCode) yield {
            smsCodeFormR.PropsVal(
              smsCode   = smsCode,
              disabled  = props.s2SmsCode.submitReq.isPending,
            )
          }
        }( smsCodeFormR.component.apply )(implicitly, OptFastEq.Wrapped(smsCodeFormR.SmsCodeFormRPropsValFastEq)),

        // Рендер ошибки запроса
        s.submitReqExC { errorSnackR.component.apply },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        phoneNumberC = propsProxy.connect(_.s0Creds.phone.value)( FastEqUtil.RefValFastEq ),
        submitReqExC = propsProxy.connect(_.s2SmsCode.submitReq.exceptionOrNull)( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

}
