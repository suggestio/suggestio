package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiColorTypes, MuiIconButton, MuiIconButtonProps, MuiLink, MuiLinkProps, MuiToolTip, MuiToolTipProps}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactDiodeUtil
import io.suggest.routes.routes
import io.suggest.sc.ScConstants
import io.suggest.spa.{FastEqUtil, SioPages}
import io.suggest.xplay.json.PlayJsonSjsUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 20:10
  * Description: Компонент ссылки на выдачу узла.
  */
final class NodeScLinkR(
                         crCtxP               : React.Context[MCommonReactCtx],
                       ) {

  type Props = String

  class Backend($: BackendScope[Props, Unit]) {

    def render(nodeId: String): VdomElement = {
      MuiToolTip(
        new MuiToolTipProps {
          override val title = crCtxP.message( MsgCodes.`Showcase` ).rawNode
        }
      )(
        MuiLink(
          new MuiLinkProps {
            val target = "_blank"
            val href = ScConstants.ScJsState.fixJsRouterUrl(
              routes.controllers.sc.ScSite.geoSite(
                PlayJsonSjsUtil.toNativeJsonObj(
                  Json.toJsObject(
                    SioPages.Sc3(
                      nodeId = Some( nodeId ),
                    )
                  )
                )
              ).url
            )
          }
        )(
          MuiIconButton(
            new MuiIconButtonProps {
              override val color = MuiColorTypes.primary
            }
          )(
            Mui.SvgIcons.Apps()(),
          )
        )
      )
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.propsFastEqShouldComponentUpdate( FastEqUtil.AnyRefFastEq ) )
    .build

}
