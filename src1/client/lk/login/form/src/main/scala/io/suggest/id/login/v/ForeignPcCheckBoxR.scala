package io.suggest.id.login.v

import chandu0101.scalajs.react.components.materialui.{MuiCheckBox, MuiCheckBoxProps, MuiFormControlLabel, MuiFormControlLabelProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.SetForeignPc
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, React, ReactEventFromInput, ScalaComponent}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil

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
                        ) {

  type Props_t  = Some[Boolean]
  type Props    = ModelProxy[Props_t]


  case class State(
                    checkedSomeC    : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onForeignPcChange( event: ReactEventFromInput, checked: Boolean ): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SetForeignPc(checked) )
    private val _onForeignPcChangeCbF = ReactCommonUtil.cbFun2ToJsCb( _onForeignPcChange )


    def render(s: State): VdomElement = {
      MuiFormControlLabel {
        val checkBox = s.checkedSomeC { checkedSomeProxy =>
          MuiCheckBox(
            new MuiCheckBoxProps {
              @JSName("onChange")
              override val onChange2 = _onForeignPcChangeCbF
              override val checked = js.defined( checkedSomeProxy.value.value )
            }
          )
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
