package io.suggest.lk.nodes.form.r.menu

import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.lk.nodes.form.m.NodeMenuBtnClick
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/** Компонентик кнопки меню узла. */
object NodeMenuBtnR {

  type Props = ModelProxy[_]

  class Backend($: BackendScope[Props, Unit]) {

    private def onNodeMenuBtnClick(e: ReactEvent): Callback = {
      e.stopPropagationCB >> dispatchOnProxyScopeCB(
        $.asInstanceOf[BackendScope[ModelProxy[Any], Unit]],   // TODO Какая-то шляпа тут с типами. _ не подходит почему-то.
        NodeMenuBtnClick
      )
    }

    def render: ReactElement = {
      <.span(
        ^.`class`  := Css.Lk.Nodes.Menu.BTN,
        ^.onClick ==> onNodeMenuBtnClick,
        // TODO Добавить поддержку mouse enter, чтобы по менюшке кликать не приходилось.
        //^.onClick ==> onNodeDeleteClick,
        //^.title    := (Messages( MsgCodes.`Delete` ) + HtmlConstants.ELLIPSIS)
        ". . ."
      )
    }

  }

  val component = ReactComponentB[Props]("MenuBtn")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mproxy: Props) = component(mproxy)

}