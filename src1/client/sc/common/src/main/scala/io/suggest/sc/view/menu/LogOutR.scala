package io.suggest.sc.view.menu

import scalacss.ScalaCssReact._
import com.materialui.{MuiListItem, MuiListItemProps, MuiListItemText}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.LogoutStep
import io.suggest.lk.m.CsrfTokenEnsure
import io.suggest.proto.http.client.HttpClient
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.routes.routes
import io.suggest.sc.model.MScRoot
import io.suggest.sc.view.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.2020 23:07
  * Description: Компонент пункта меню с кнопкой выхода из системы.
  */
class LogOutR(
               menuItemR   : MenuItemR,
               crCtxP      : React.Context[MCommonReactCtx],
               scCssP      : React.Context[ScCss],
             ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    isShownSomeC: ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onClick = ReactCommonUtil.cbFun1ToJsCb { e: ReactEvent =>
      ReactCommonUtil.preventDefaultCB(e) >>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CsrfTokenEnsure(
        onComplete = Some( LogoutStep().toEffectPure ),
      ))
    }

    def render(s: State): VdomElement = {
      s.isShownSomeC { isShownSomeProxy =>
        val isShown = isShownSomeProxy.value.value
        ReactCommonUtil.maybeEl( isShown ) {
          val R = ScCssStatic.Menu.Rows

          // Ссылка на вход или на личный кабинет
          val listItem = MuiListItem(
            new MuiListItemProps {
              override val disableGutters = menuItemR.DISABLE_GUTTERS
              override val button = true
              override val onClick = _onClick
              override val classes = menuItemR.MENU_LIST_ITEM_CSS
            }
          )(
            MuiListItemText()(
              {
                val span0 = <.span(
                  R.rowContent,
                  crCtxP.message( MsgCodes.`Logout.account` ),
                  //HtmlConstants.ELLIPSIS,
                )
                scCssP.consume { scCss =>
                  span0(
                    scCss.fgColor,
                  )
                }
              }
            )
          )

          <.a(
            R.rowLink,
            ^.href := HttpClient.mkAbsUrlIfPreferred( routes.controllers.Ident.logout().url ),
            listItem,
          )
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isShownSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.index.isLoggedIn )
        }(FastEq.AnyRefEq),
      )
    }
    .renderBackend[Backend]
    .build

}
