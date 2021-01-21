package io.suggest.sc.v.dia.dlapp

import com.materialui.{Mui, MuiListItem, MuiListItemClasses, MuiListItemProps, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps, MuiSvgIconClasses, MuiSvgIconProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.menu.DlAppOpen
import io.suggest.sc.m.MScRoot
import io.suggest.sc.v.menu.MenuItemR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 10:09
  * Description: wrap-компонент пункта меню, содержащего данные для скачивания мобильного приложения.
  */
class DlAppMenuItemR(
                      menuItemR          : MenuItemR,
                      scCssP             : React.Context[ScCss],
                      crCtxP             : React.Context[MCommonReactCtx],
                    ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    isOpenedSomeC           : ReactConnectProxy[Some[Boolean]],
                    nodeIdOptC              : ReactConnectProxy[Option[String]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private lazy val _onOpenCloseClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      $.props >>= { propsProxy: Props =>
        val isOpenedNow = propsProxy.value.index.menu.dlApp.opened
        // Открыть/закрыть диалог
        propsProxy.dispatchCB( DlAppOpen( !isOpenedNow ) ) >>
        // И скрыть менюшку, если открыта.
        propsProxy.dispatchCB( SideBarOpenClose(MScSideBars.Menu, open = OptionUtil.SomeBool.someFalse) )
      }
    }


    def render(s: State): VdomElement = {
      val R = ScCssStatic.Menu.Rows

      // Основной пункт меню.
      val headlineChs = List[VdomElement](
        <.span(
          R.rowContent,
          crCtxP.message( MsgCodes.`Application` ),
        ),
        // Иконка закачки.
        Mui.SvgIcons.GetApp {
          val css = new MuiSvgIconClasses {
            override val root = R.rightIcon.htmlClass
          }
          new MuiSvgIconProps {
            override val classes = css
          }
        }(),
      )

      MuiListItem {
        val css = new MuiListItemClasses {
          override val root = Css.flat(
            menuItemR.MENU_LIST_ITEM_CSS_ROOT,
            R.rowLink.htmlClass,
          )
        }
        new MuiListItemProps {
          override val disableGutters = menuItemR.DISABLE_GUTTERS
          override val button         = true
          override val onClick        = _onOpenCloseClickCbF
          override val classes        = css
        }
      } (
        scCssP.consume { scCss =>
          MuiListItemText {
            val css = new MuiListItemTextClasses {
              override val root = scCss.fgColor.htmlClass
            }
            new MuiListItemTextProps {
              override val classes = css
            }
          } ( headlineChs: _* )
        },
      )
    }

  }


  // lazy - потому что в cordova этот пункт не нужен.
  lazy val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsOptProxy =>
      State(
        isOpenedSomeC = propsOptProxy.connect { props =>
          OptionUtil.SomeBool( props.index.menu.dlApp.opened )
        },

        nodeIdOptC = propsOptProxy.connect { props =>
          props.index.state.rcvrId
        },
      )
    }
    .renderBackend[Backend]
    .build

}
