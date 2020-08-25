package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiColorTypes, MuiIconButton, MuiIconButtonProps, MuiLink, MuiLinkProps, MuiToolTip, MuiToolTipProps}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.NodesDiConf
import io.suggest.proto.http.client.HttpClient
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.routes.routes
import io.suggest.sc.ScConstants
import io.suggest.sjs.common.empty.JsOptionUtil
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
                         diConfig             : NodesDiConf,
                       ) {

  type Props = String

  class Backend($: BackendScope[Props, Unit]) {

    private lazy val _onLinkClick = ReactCommonUtil.cbFun1ToJsCb { e: ReactEvent =>
      var cb = e.stopPropagationCB

      // Если есть связь с выдачей, то отправить в роутер выдачи новое состояние:
      for (scRouterCtl <- diConfig.scRouterCtlOpt) {
        cb = cb >> e.preventDefaultCB >> {
          $.props >>= { nodeId: Props =>
            scRouterCtl.set(
              SioPages.Sc3(
                nodeId = Some( nodeId ),
              )
            )
          }
        }
      }

      // Если форма поддерживает закрытие/сокрытие, то сделать это:
      for (closeFormCb <- diConfig.closeForm)
        cb = cb >> closeFormCb

      cb
    }

    def render(nodeId: String): VdomElement = {
      MuiToolTip(
        new MuiToolTipProps {
          override val title = crCtxP.message( MsgCodes.`Showcase` ).rawNode
        }
      )(
        MuiLink(
          new MuiLinkProps {
            val target = JsOptionUtil.maybeDefined( diConfig.scRouterCtlOpt.isEmpty )( "_blank" )
            override val onClick = _onLinkClick
            val href = HttpClient.mkAbsUrlIfPreferred(
              ScConstants.ScJsState.fixJsRouterUrl(
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
