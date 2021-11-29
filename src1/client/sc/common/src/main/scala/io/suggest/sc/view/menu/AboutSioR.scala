package io.suggest.sc.view.menu

import diode.react.{ModelProxy, ReactConnectProxy}
import com.materialui.{MuiListItem, MuiListItemText}
import diode.data.Pot
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.proto.http.client.HttpClient
import io.suggest.react.ReactDiodeUtil
import io.suggest.routes.routes
import io.suggest.sc.ScConstants.ScJsState
import io.suggest.sc.model.{MScRoot, ResetUrlRoute}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sc.view.styl.{ScCss, ScCssStatic}
import io.suggest.spa.SioPages
import io.suggest.xplay.json.PlayJsonSjsUtil
import japgolly.scalajs.react.extra.router.SetRouteVia
import play.api.libs.json.Json
import scalacss.ScalaCssReact._
import japgolly.univeq._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 16:04
  * Description: Пункт "О проекте" в левом меню выдачи.
  */
class AboutSioR(
                 menuItemR              : MenuItemR,
                 scCssP                 : React.Context[ScCss],
                 crCtxProv              : React.Context[MCommonReactCtx],
               ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    jsRoutesC: ReactConnectProxy[Pot[routes.type]],
                  )

  private def _mkAboutUrlState( nodeIdOpt: Option[String] ) = SioPages.Sc3(
    nodeId = nodeIdOpt,
  )

  class Backend($: BackendScope[Props, State]) {

    private def _onAboutLinkClick(nodeId: String)(e: ReactMouseEvent): Callback = {
      Callback.unless( ReactMouseEvent targetsNewTab_? e ) {
        e.preventDefaultCB >> ReactDiodeUtil.dispatchOnProxyScopeCB( $,
          // We do not use routerCtl.set() here, because AboutSioR must be portable and live without external SPA-router.
          ResetUrlRoute(
            mods = Some { _ =>
              _mkAboutUrlState( Some(nodeId) )
            },
            via = SetRouteVia.HistoryPush,
          )
        )
      }
    }

    def render(s: State): VdomElement = {
      import ScCssStatic.Menu.{Rows => R}

      lazy val _listItem = MuiListItem(
        menuItemR.MENU_LIST_ITEM_PROPS
      )(
        MuiListItemText()(
          {
            val span0 = <.span(
              R.rowContent,
              crCtxProv.message( MsgCodes.`Suggest.io.Project` ),
            )
            scCssP.consume { scCss =>
              span0(
                scCss.fgColor,
              )
            }
          }
        )
      )

      crCtxProv.consume { crCtx =>
        // TODO aboutNodeId надо унести из конфига в messages. Тогда, при переключении языков, будет меняться узел. Или придумать стабильные id по разным языкам, без messages.
        // Это типа ссылка <a>, но уже с выставленным href + go-событие.
        val msgCode = MsgCodes.`About.sio.node.id`
        val nodeIdOpt = Option( crCtx.messages( msgCode ) )
          .filter( _ !=* msgCode )

        s.jsRoutesC { jsRoutesProxy =>
          <.a(
            nodeIdOpt.whenDefined { nodeId =>
              TagMod(
                ^.onClick ==> _onAboutLinkClick( nodeId ),

                // Also allow render without JS-router available.
                jsRoutesProxy.value.toOption.whenDefined { jsRoutes =>
                  ^.href := ScJsState.fixJsRouterUrl(
                    HttpClient.mkAbsUrlIfPreferred(
                      jsRoutes.controllers.sc.ScSite.geoSite(
                        PlayJsonSjsUtil.toNativeJsonObj(
                          Json
                            .toJsObject( _mkAboutUrlState(nodeIdOpt) ) )
                      )
                        .absoluteURL( HttpClient.PREFER_SECURE_URLS ) )
                  )
                },
              )
            },

            R.rowLink,
            ^.title := crCtx.messages( MsgCodes.`Suggest.io._transcription` ),
            _listItem,
          )
        }
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        jsRoutesC = propsProxy.connect( _.internals.jsRouter.jsRoutes ),
      )
    }
    .renderBackend[Backend]
    .build

}
