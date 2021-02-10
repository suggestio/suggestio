package io.suggest.sc.v.menu

import com.materialui.{MuiListItem, MuiListItemProps, MuiListItemText}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.{MScRoot, ScNodesShowHide}
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.2020 16:20
  * Description: Пункт меню для доступа к форме управления узлами.
  */
class ScNodesBtnR(
                   menuItemR     : MenuItemR,
                   scCssP        : React.Context[ScCss],
                   crCtxProv     : React.Context[MCommonReactCtx],
                 ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    private lazy val _onMenuItemClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScNodesShowHide( visible = true ) ) >>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SideBarOpenClose( bar = MScSideBars.Menu, open = Some(false) ) )
    }

    val render: VdomElement = {
      import ScCssStatic.Menu.{Rows => R}

      MuiListItem(
        new MuiListItemProps {
          override val disableGutters = menuItemR.DISABLE_GUTTERS
          override val button = true
          override val onClick = _onMenuItemClick
          override val classes = menuItemR.MENU_LIST_ITEM_CSS
        }
      )(
        MuiListItemText()(
          {
            val span0 = <.span(
              R.rowContent,
              crCtxProv.message( MsgCodes.`Nodes.management` ),
            )
            scCssP.consume { scCss =>
              span0(
                scCss.fgColor,
              )
            }
          },

          // TODO Вывести счётких текущих видимых маячков.
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
