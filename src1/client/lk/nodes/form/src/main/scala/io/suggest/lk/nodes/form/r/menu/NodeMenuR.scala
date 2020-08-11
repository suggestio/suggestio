package io.suggest.lk.nodes.form.r.menu

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.NodeDeleteClick
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.17 12:49
  * Description: Компонент опциональной менюшки текущего узла.
  */
class NodeMenuR(
                 crCtxP: React.Context[MCommonReactCtx],
               ) {

  lazy val _deleteMsg = crCtxP.consume { crCtx =>
    crCtx.messages( MsgCodes.`Delete` ) + HtmlConstants.ELLIPSIS
  }

  type Props = ModelProxy[_]

  class Backend($: BackendScope[Props, Unit]) {

    private val _onDeleteClick: Callback = {
      dispatchOnProxyScopeCB( $, NodeDeleteClick )
    }

    def render(p: Props): VdomElement = {
      <.div(
        ^.`class` := Css.Lk.Nodes.Menu.CONT,

        <.div(
          ^.`class` := Css.Lk.Nodes.Menu.ITEM,
          ^.onClick --> _onDeleteClick,
          _deleteMsg,
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
