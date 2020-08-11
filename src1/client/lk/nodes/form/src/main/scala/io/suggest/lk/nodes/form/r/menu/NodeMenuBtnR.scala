package io.suggest.lk.nodes.form.r.menu

import com.materialui.{Mui, MuiColorTypes, MuiIconButton, MuiIconButtonClasses, MuiIconButtonProps}
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.lk.nodes.form.m.NodeMenuBtnClick
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/** Компонентик кнопки меню узла. */
class NodeMenuBtnR {

  type Props = ModelProxy[_]

  class Backend($: BackendScope[Props, Unit]) {

    private def _onNodeMenuBtnClick(e: ReactEvent): Callback = {
      e.stopPropagationCB >> dispatchOnProxyScopeCB( $, NodeMenuBtnClick )
    }
    private lazy val _onNodeMenuBtnClickF = ReactCommonUtil.cbFun1ToJsCb( _onNodeMenuBtnClick )

    def render: VdomElement = {
      <.span(
        ^.`class`  := Css.Lk.Nodes.Menu.BTN,
        MuiIconButton {
          val css = new MuiIconButtonClasses {
            override val root = Css.Lk.Nodes.Menu.BTN
          }
          new MuiIconButtonProps {
            override val color = MuiColorTypes.primary
            override val onClick = _onNodeMenuBtnClickF
            override val classes = css
          }
        } (
          Mui.SvgIcons.Menu()(),
        ),
      )
      /*
      <.span(
        ^.onClick ==> _onNodeMenuBtnClick,
        ". . ."
      )
      */
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
