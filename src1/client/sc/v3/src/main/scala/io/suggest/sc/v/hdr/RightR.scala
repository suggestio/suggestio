package io.suggest.sc.v.hdr

import com.materialui.{Mui, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps}
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.v.styl.ScCssStatic
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 16:56
  * Description: Компонент кнопки, указывающей вправо (или "вперёд").
  */
class RightR {

  type Props_t = None.type
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    private val _onClosePanelBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB($, SideBarOpenClose(MScSideBars.Search, open = OptionUtil.SomeBool.someFalse))
    }

    val render: VdomElement = {
      MuiIconButton {
        new MuiIconButtonProps {
          override val onClick = _onClosePanelBtnClickJsCbF
        }
      }(
        Mui.SvgIcons.ArrowForwardIosOutlined()()
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(noneProxy: Props) = component( noneProxy )

}
