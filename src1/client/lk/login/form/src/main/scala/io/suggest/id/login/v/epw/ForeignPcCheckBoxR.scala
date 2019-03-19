package io.suggest.id.login.v.epw

import chandu0101.scalajs.react.components.materialui.{MuiCheckBox, MuiCheckBoxClasses, MuiCheckBoxProps, MuiFormControlLabel, MuiFormControlLabelProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.EpwSetForeignPc
import io.suggest.id.login.v.LoginFormCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, React, ReactEventFromInput, ScalaComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.19 18:03
  * Description: wrap() react-компонент галочки чужой пекарни.
  */
class ForeignPcCheckBoxR(
                          commonReactCtx      : React.Context[MCommonReactCtx],
                          loginFormCssCtx     : React.Context[LoginFormCss],
                        ) {

  type Props_t  = Some[Boolean]
  type Props    = ModelProxy[Props_t]


  case class State(
                    checkedSomeC    : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onForeignPcChange( event: ReactEventFromInput, checked: Boolean ): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EpwSetForeignPc(checked) )
    private val _onForeignPcChangeCbF = ReactCommonUtil.cbFun2ToJsCb( _onForeignPcChange )


    def render(s: State): VdomElement = {
      MuiFormControlLabel {
        val checkBox = s.checkedSomeC { checkedSomeProxy =>
          loginFormCssCtx.consume { loginFormCss =>
            val cbCss = new MuiCheckBoxClasses {
              override val root = loginFormCss.epwFormControl.htmlClass
            }
            MuiCheckBox(
              new MuiCheckBoxProps {
                @JSName("onChange")
                override val onChange2 = _onForeignPcChangeCbF
                override val checked = js.defined( checkedSomeProxy.value.value )
                override val classes = cbCss
              }
            )
          }
        }

        val labelText = commonReactCtx.consume { crCtx =>
          crCtx.messages( MsgCodes.`Not.my.pc` )
        }

        new MuiFormControlLabelProps {
          override val control = checkBox.rawElement
          override val label = labelText.rawNode
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        checkedSomeC = propsProxy.connect(identity)( FastEqUtil.RefValFastEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
