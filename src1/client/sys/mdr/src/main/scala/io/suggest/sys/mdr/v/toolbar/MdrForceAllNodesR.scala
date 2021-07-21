package io.suggest.sys.mdr.v.toolbar

import com.materialui.{MuiFormControlLabel, MuiFormControlLabelProps, MuiSwitch, MuiSwitchProps, MuiToolTip, MuiToolTipProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sys.mdr.m.SetForceAllNodes
import japgolly.scalajs.react.raw.React

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.10.18 18:34
  * Description: Компонент управления переключением режима формы.
  */
class MdrForceAllNodesR {

  case class PropsVal(
                       checked    : Boolean,
                       disabled   : Boolean
                     )

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    private def _onChange(event: ReactEventFromInput, checked: Boolean): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SetForceAllNodes(checked) )
    private lazy val _onChangeJsCbF = ReactCommonUtil.cbFun2ToJsCb( _onChange )


    def render(isEnabledOptProxy: Props): VdomElement = {
      isEnabledOptProxy.value.whenDefinedEl { props =>
        <.span(
          HtmlConstants.PIPE,
          HtmlConstants.NBSP_STR,

          // Чек-бокс
          MuiToolTip(
            new MuiToolTipProps {
              override val title: React.Node = Messages( MsgCodes.`Moderate.requests.from.all.nodes` )
            }
          )(
            <.span(
              MuiFormControlLabel {
                val switchEl = MuiSwitch(
                  new MuiSwitchProps {
                    override val checked    = js.defined( props.checked )
                    @JSName("onChange")
                    override val onChange2  = _onChangeJsCbF
                    override val disabled = props.disabled
                  }
                )
                new MuiFormControlLabelProps {
                  override val control = switchEl.rawElement
                  override val label = js.defined(
                    Messages( MsgCodes.`Moderate.all.nodes` )
                  )
                  override val disabled = props.disabled
                }
              },
            )
          )
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( props: Props ) = component( props )

}
