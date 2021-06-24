package io.suggest.lk.r.sms

import com.materialui.{MuiInputValue_t, MuiTextField, MuiTextFieldProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.m.{SmsCodeBlur, SmsCodeSet}
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.lk.m.sms.MSmsCodeS
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.19 11:02
  * Description: Подформа для ввода смс-кода подтверждения.
  */
class SmsCodeFormR(
                    crCtxProv    : React.Context[MCommonReactCtx],
                  ) {

  case class PropsVal(
                       smsCode    : MSmsCodeS,
                       disabled   : Boolean,
                     )
  implicit object SmsCodeFormRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.smsCode ===* b.smsCode) &&
      (a.disabled ==* b.disabled)
    }
  }


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  case class State(
                    isShownC        : ReactConnectProxy[Option[PropsVal]],
                    codeInputC      : ReactConnectProxy[Option[MTextFieldS]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private val _onSmsCodeTypingCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val typed2 = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB($, SmsCodeSet(smsCode = typed2) )
    }

    private val _onInputBlurCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactFocusEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB($, SmsCodeBlur)
    }


    def render(s: State): VdomElement = {

      val labelText = crCtxProv.message( MsgCodes.`Code.from.sms` )

      // Поле ввода отправленного смс-кода.
      val smsCodeInput = s.codeInputC { codeInputProxy =>
        codeInputProxy.value.whenDefinedEl { v =>
          MuiTextField(
            new MuiTextFieldProps {
              override val onChange = _onSmsCodeTypingCbF
              override val value = js.defined {
                v.value: MuiInputValue_t
              }
              override val label        = labelText.rawNode
              override val `type`       = HtmlConstants.Input.tel
              override val error        = !v.isValid
              override val onBlur       = _onInputBlurCbF
              override val disabled     = !v.isEnabled
              override val variant      = MuiTextField.Variants.standard
            }
          )()
        }
      }

      // TODO Ссылка, чтобы выслать повторно смс-код.

      // Форма отображается или нет?
      s.isShownC { isShownProxy =>
        isShownProxy.value.whenDefinedEl { _ =>
          <.div(
            smsCodeInput
          )
        }
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsOptProxy =>
      State(
        isShownC = propsOptProxy.connect(identity)( OptFastEq.IsEmptyEq ),

        codeInputC = propsOptProxy.connect { propsOpt =>
          for (props <- propsOpt) yield {
            val isEnabled2 = !props.disabled
            val text = props.smsCode.typed
            if (text.isEnabled ==* isEnabled2) text
            else (MTextFieldS.isEnabled set isEnabled2)(text)
          }
        }( OptFastEq.Wrapped(MTextFieldS.MTextFieldSFastEq) ),

      )
    }
    .renderBackend[Backend]
    .build

}
