package io.suggest.lk.nodes.form.r.menu

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.nodes.form.m.{MNodeMenuS, NodeDeleteClick}
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.17 12:49
  * Description: Компонент опциональной менюшки текущего узла.
  */
object NodeMenuR {

  type Props = ModelProxy[Option[MNodeMenuS]]

  class Backend($: BackendScope[Props, _]) {

    def deleteClick: Callback = {
      dispatchOnProxyScopeCB( $, NodeDeleteClick )
    }

    def render(p: Props): ReactElement = {
      val v = p()
      for (_ <- v) yield {
        <.div(
          ^.`class` := Css.Lk.Nodes.Menu.CONT,

          <.div(
            ^.`class` := Css.Lk.Nodes.Menu.ITEM,
            ^.onClick --> deleteClick,
            Messages("Delete") + HtmlConstants.ELLIPSIS
          )

        ): ReactElement
      }
    }

  }


  val component = ReactComponentB[Props]("NodeMenu")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(nodeMenuOptProxy: Props) = component(nodeMenuOptProxy)

}
