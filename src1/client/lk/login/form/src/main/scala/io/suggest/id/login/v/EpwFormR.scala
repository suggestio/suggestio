package io.suggest.id.login.v

import chandu0101.scalajs.react.components.materialui.{MuiPaper, MuiTextField, MuiTextFieldProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.MsgCodes
import io.suggest.id.login.m.{EpwSetName, EpwSetPassword, MEpwLoginS}
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 16:49
  * Description: Форма входа по имени и паролю.
  */
class EpwFormR {

  type Props_t = MEpwLoginS
  type Props = ModelProxy[Props_t]


  case class State(
                    nameC               : ReactConnectProxy[String],
                    passwordC           : ReactConnectProxy[String],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onNameChange(event: ReactEventFromInput): Callback = {
      val name2 = event.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EpwSetName(name2) )
    }
    private val _onNameChangeCbF = ReactCommonUtil.cbFun1ToJsCb( _onNameChange )


    private def _onPasswordChange(event: ReactEventFromInput): Callback = {
      val password2 = event.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EpwSetPassword(password2) )
    }
    private val _onPasswordChangeCbF = ReactCommonUtil.cbFun1ToJsCb( _onPasswordChange )


    def render(s: State): VdomElement = {
      MuiPaper()(

        // Поле имени пользователя:
        s.nameC { nameProxy =>
          MuiTextField(
            new MuiTextFieldProps {
              override val value = js.defined {
                nameProxy.value
              }
              override val onChange = _onNameChangeCbF
              override val placeholder = Messages( MsgCodes.`Name` )
            }
          )
        },

        // Поле ввода пароля:
        s.passwordC { passwordProxy =>
          MuiTextField(
            new MuiTextFieldProps {
              override val value = js.defined {
                passwordProxy.value
              }
              override val onChange = _onPasswordChangeCbF
              override val placeholder = Messages( MsgCodes.`Password` )
            }
          )
        }

      )
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        nameC       = propsProxy.connect(_.name)( FastEq.AnyRefEq ),
        passwordC   = propsProxy.connect(_.password)( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
