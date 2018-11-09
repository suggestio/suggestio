package io.suggest.sc.v.hdr

import chandu0101.scalajs.react.components.materialui.{Mui, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps}
import diode.react.ModelProxy
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 16:56
  * Description: Компонент кнопки, указывающей вправо (или "вперёд").
  */
class RightR(
              getScCssF: GetScCssF
            ) {

  type Props_t = None.type
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    private def _onClosePanelBtnClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, SideBarOpenClose(MScSideBars.Search, open = false))
    private val _onClosePanelBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onClosePanelBtnClick )


    def render(propsProxy: Props): VdomElement = {
      MuiIconButton {
        val cssClasses = new MuiIconButtonClasses {
          override val root = getScCssF().Search.hideBtn.htmlClass
        }
        new MuiIconButtonProps {
          override val onClick = _onClosePanelBtnClickJsCbF
          override val classes = cssClasses
        }
      }(
        Mui.SvgIcons.ArrowForwardIos()()
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
