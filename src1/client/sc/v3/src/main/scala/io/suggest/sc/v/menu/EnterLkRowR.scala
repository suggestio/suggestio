package io.suggest.sc.v.menu

import com.materialui.{MuiListItem, MuiListItemClasses, MuiListItemProps, MuiListItemText}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.routes.IJsRouter
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.{MScRoot, ScLoginFormShowHide}
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import org.scalajs.dom
import scalacss.ScalaCssReact._

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.18 18:19
  * Description: Компонент строки меню с ссылкой для логина в s.io.
  */
class EnterLkRowR(
                   menuItemR     : MenuItemR,
                   scCssP        : React.Context[ScCss],
                   crCtxProv     : React.Context[MCommonReactCtx],
                   jsRouterOptP  : React.Context[Option[IJsRouter]],
                 )
  extends Log
{

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  private object Modes {
    val _NOT_LOGGED_IN    = None
    val _LOGGED_IN        = Some("")
  }

  case class State(
                    mode              : ReactConnectProxy[Option[String]],
                    internalLogin     : ReactConnectProxy[Option[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private def _onLoginClick(e: ReactEvent): Callback = {
      ReactCommonUtil.preventDefaultCB(e) >>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScLoginFormShowHide(true) )
    }

    def render(s: State): VdomElement = {
      val R = ScCssStatic.Menu.Rows

      // Ссылка на вход или на личный кабинет
      val listItem = MuiListItem(
        menuItemR.MENU_LIST_ITEM_PROPS
      )(
        MuiListItemText()(
          s.mode { modeProxy =>
            val msgCode = modeProxy.value match {
              case Modes._NOT_LOGGED_IN     => MsgCodes.`Login.page.title`
              case Modes._LOGGED_IN         => MsgCodes.`Personal.cabinet`
              case _                        => MsgCodes.`Go.to.node.ads`
            }
            val textSpan = <.span(
              R.rowContent,
              crCtxProv.message( msgCode ),
            )
            scCssP.consume { scCss =>
              textSpan(
                scCss.fgColor,
              )
            }
          },
        )
      )

      s.internalLogin { internalLoginProxy =>
        internalLoginProxy.value.whenDefinedEl { isInternalLogin =>
          jsRouterOptP.consume { jsRouterOpt =>
            val linkAcc = <.a(
              R.rowLink,
              listItem,
            )

            // Тут вложенные connect'ы, это снижает производительности. Но internalLogin почти не изменяется, поэтому не канает.
            val intLoginTm = if (isInternalLogin) {
              ^.onClick ==> _onLoginClick
            } else {
              TagMod.empty
            }

            s.mode { modeProxy =>
              val hrefTm = jsRouterOpt.whenDefined { jsRouter =>
                // Сборка ссылки для входа туда, куда наверное хочет попасть пользователь.
                val route = modeProxy.value match {
                  case Modes._NOT_LOGGED_IN     => jsRouter.controllers.Ident.loginFormPage()
                  case Modes._LOGGED_IN         => jsRouter.controllers.Ident.rdrUserSomewhere()
                  case Some( nodeId )           => jsRouter.controllers.LkAds.adsPage( nodeId )
                }
                ^.href := HttpClient.mkAbsUrlIfPreferred( route.url )
              }
              linkAcc(
                hrefTm,
                intLoginTm,
              )
            }
          }
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        mode = propsProxy.connect { props =>
          val rcvrId = props.index.state.rcvrId
            .filter { _ =>
              props.index.resp.exists(_.isMyNode contains[Boolean] true)
            }
          rcvrId.fold {
            val isLoggedIn = props.index.isLoggedIn
            if (isLoggedIn) Modes._LOGGED_IN
            else Modes._NOT_LOGGED_IN
          } { _ =>
            rcvrId
          }
        },

        internalLogin = {
          val isAvail = try {
            (dom.window.location.protocol startsWith HttpConst.Proto.HTTPS) ||
            scalajs.LinkingInfo.developmentMode
          } catch {
            case ex: Throwable =>
              logger.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, ex )
              true
          }

          propsProxy.connect { props =>
            val isLoggedIn = props.index.isLoggedIn
            val isCordova = props.dev.platform.isCordova
            // если logged-in + cordova => None, т.к. ссылки в ЛК быть не должно.
            OptionUtil.maybeOpt( !(isLoggedIn && isCordova) ) {
              // в cordova требуется internal login, т.е. допускается только встроенная форма логина:
              val r = !isLoggedIn && (isCordova || isAvail)
              OptionUtil.SomeBool( r )
            }
          }
        },

      )
    }
    .renderBackend[Backend]
    .build

}
