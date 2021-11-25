package io.suggest.sc.view.hdr

import com.materialui.{Mui, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps}
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.model.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.view.styl.ScCssStatic
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 16:56
  * Description: Компонент кнопки, указывающей вправо (или "вперёд").
  */
class LeftR {

  type Props_t = None.type
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    private def _onClosePanelBtnClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, SideBarOpenClose(MScSideBars.Menu, open = OptionUtil.SomeBool.someFalse))
    private val _onClosePanelBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onClosePanelBtnClick )


    def render(propsProxy: Props): VdomElement = {
      MuiIconButton {
        new MuiIconButtonProps {
          override val onClick = _onClosePanelBtnClickJsCbF
          override val classes = new MuiIconButtonClasses {
            override val root = ScCssStatic.Header.Buttons.mui5iconBtnPatch.htmlClass
          }
        }
      }(
        Mui.SvgIcons.ArrowBackIosOutlined()()
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
