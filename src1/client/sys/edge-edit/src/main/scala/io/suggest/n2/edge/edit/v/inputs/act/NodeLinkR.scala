package io.suggest.n2.edge.edit.v.inputs.act

import com.materialui.{Mui, MuiLink, MuiLinkProps, MuiToolTip, MuiToolTipProps}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.routes.routes
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.01.2020 17:51
  * Description: Стрелка-ссылка на sys-страницу указанного узла.
  */
class NodeLinkR(
                 crCtxProv: React.Context[MCommonReactCtx],
               ) {

  val component = ScalaComponent
    .builder[String]( getClass.getSimpleName )
    .render_P { nodeId =>
      MuiToolTip {
        new MuiToolTipProps {
          override val title = crCtxProv.message( MsgCodes.`Go.to.node.0`, nodeId ).rawNode
          val visibility = {
            if (nodeId.isEmpty) "hidden"
            else "visible"
          }
        }
      } (
        MuiLink(
          new MuiLinkProps {
            val href = routes.controllers.SysMarket.showAdnNode( nodeId ).url
          }
        )(
          Mui.SvgIcons.ArrowForward()(),
        )
      )
    }
    .build

}
