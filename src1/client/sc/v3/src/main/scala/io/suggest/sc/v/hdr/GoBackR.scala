package io.suggest.sc.v.hdr

import diode.react.ModelProxy
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.inx.{GoToPrevIndexView, MIndexView}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import ReactCommonUtil.Implicits._
import com.materialui.{Mui, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps, MuiToolTip, MuiToolTipProps}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.sc.v.styl.ScCssStatic

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 16:56
  * Description: Компонент кнопки, указывающей вправо (или "вперёд").
  */
class GoBackR(
               crCtxProv     : React.Context[MCommonReactCtx],
             ) {

  type Props_t = Option[MIndexView]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    private def _onBtnClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GoToPrevIndexView)
    private val _onBtnClickJsCbF = ReactCommonUtil.cbFun1ToJsCb( _onBtnClick )


    def render(propsProxy: Props): VdomElement = {
      propsProxy.value.whenDefinedEl { miv =>
        MuiToolTip {
          val (msgCode, msgArgs) = miv.name.fold {
            MsgCodes.`Go.back` -> List.empty[js.Any]
          } { prevNodeName =>
            MsgCodes.`Go.back.to.0` -> List[js.Any]( prevNodeName )
          }
          val titleText = crCtxProv.message( msgCode, msgArgs: _*)

          new MuiToolTipProps {
            override val title = titleText.rawNode
          }
        } (
          MuiIconButton {
            val cssClasses = new MuiIconButtonClasses {
              override val root = ScCssStatic.Header.Buttons.backBtn.htmlClass
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
