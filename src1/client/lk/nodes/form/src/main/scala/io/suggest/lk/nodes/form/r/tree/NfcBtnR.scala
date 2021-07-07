package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiColorTypes, MuiIconButton, MuiIconButtonProps, MuiToolTip, MuiToolTipProps}
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.lk.nodes.form.m.NfcDialog
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

class NfcBtnR {

  type Props = ModelProxy[_]


  class Backend($: BackendScope[Props, Unit]) {

    private val _onNfcBtnClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, NfcDialog(isOpen = true) )
    }

    def render(propsProxy: Props): VdomElement = {
      MuiToolTip(
        new MuiToolTipProps {
          override val title = MsgCodes.`NFC`.rawNode
        }
      )(
        MuiIconButton(
          new MuiIconButtonProps {
            override val color = MuiColorTypes.secondary
            override val onClick = _onNfcBtnClick
          }
        )(
          Mui.SvgIcons.Nfc()(),
        ),
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
