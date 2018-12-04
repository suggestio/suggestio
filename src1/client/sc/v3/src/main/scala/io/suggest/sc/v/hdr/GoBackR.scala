package io.suggest.sc.v.hdr

import chandu0101.scalajs.react.components.materialui.{Mui, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps, MuiToolTip, MuiToolTipProps}
import diode.react.ModelProxy
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.inx.{GoToPrevIndexView, MIndexView}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import ReactCommonUtil.Implicits._
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.sc.styl.GetScCssF

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 16:56
  * Description: Компонент кнопки, указывающей вправо (или "вперёд").
  */
class GoBackR(
               getScCssF: GetScCssF
             ) {

  type Props_t = Option[MIndexView]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    private def _onBtnClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GoToPrevIndexView)
    private val _onBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onBtnClick )


    def render(propsProxy: Props): VdomElement = {
      propsProxy.value.whenDefinedEl { miv =>
        val scCss = getScCssF().Header.Buttons

        MuiToolTip {
          val titleStr = miv.name.fold {
            Messages( MsgCodes.`Go.back` )
          } { prevNodeName =>
            Messages( MsgCodes.`Go.back.to.0`, prevNodeName )
          }
          new MuiToolTipProps {
            override val title: raw.React.Node = titleStr
          }
        } (
          MuiIconButton {
            val cssClasses = new MuiIconButtonClasses {
              override val root = scCss.backBtn.htmlClass
            }
            new MuiIconButtonProps {
              override val onClick = _onBtnClickJsCbF
              override val classes = cssClasses
            }
          } (
            Mui.SvgIcons.ArrowBackIosOutlined()()
          )
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(noneProxy: Props) = component( noneProxy )

}
