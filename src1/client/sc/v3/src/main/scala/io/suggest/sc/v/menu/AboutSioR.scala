package io.suggest.sc.v.menu

import diode.react.ModelProxy
import com.materialui.{MuiListItem, MuiListItemProps, MuiListItemText}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import io.suggest.spa.SioPages
import japgolly.scalajs.react.extra.router.RouterCtl
import scalacss.ScalaCssReact._

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
                 routerCtlProv          : React.Context[RouterCtl[SioPages.Sc3]],
               ) {

  type Props_t = String
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {
    def render(nodeIdProxy: Props): VdomElement = {
      import ScCssStatic.Menu.{Rows => R}

      lazy val _listItem = MuiListItem(
        menuItemR.MENU_LIST_ITEM_PROPS
      )(
        MuiListItemText()(
          scCssP.consume { scCss =>
            <.span(
              R.rowContent,
              scCss.fgColor,
              crCtxProv.message( MsgCodes.`Suggest.io.Project` ),
            )
          }
        )
      )

      crCtxProv.consume { crCtx =>
        lazy val linkChildren = List[TagMod](
          R.rowLink,
          ^.title := crCtx.messages( MsgCodes.`Suggest.io._transcription` ),
          _listItem,
        )

        // TODO aboutNodeId надо унести из конфига в messages. Тогда, при переключении языков, будет меняться узел. Или придумать стабильные id по разным языкам, без messages.
        // Это типа ссылка <a>, но уже с выставленным href + go-событие.
        routerCtlProv.consume { routerCtl =>
          routerCtl
            .link( SioPages.Sc3(
              nodeId = Some( nodeIdProxy.value )
            ))(
              linkChildren: _*
            )
        }
      }
    }
  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
