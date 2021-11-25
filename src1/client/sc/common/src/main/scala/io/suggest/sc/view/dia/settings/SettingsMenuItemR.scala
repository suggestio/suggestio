package io.suggest.sc.view.dia.settings

import com.materialui.{Mui, MuiListItem, MuiListItemProps, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps, MuiSvgIconClasses, MuiSvgIconProps}
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.model.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.model.{MScRoot, SettingsDiaOpen}
import io.suggest.sc.view.menu.MenuItemR
import io.suggest.sc.view.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 10:09
  * Description: wrap-компонент пункта меню, содержащего данные для скачивания мобильного приложения.
  */
class SettingsMenuItemR(
                         menuItemR          : MenuItemR,
                         scCssP             : React.Context[ScCss],
                         crCtxProv          : React.Context[MCommonReactCtx],
                       ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    private lazy val _onClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      $.props >>= { propsProxy: Props =>
        val isOpenedNow = propsProxy.value.dialogs.settings.opened
        // Открыть/закрыть диалог
        propsProxy.dispatchCB( SettingsDiaOpen( opened = !isOpenedNow ) ) >>
        // И скрыть менюшку, если открыта.
        propsProxy.dispatchCB( SideBarOpenClose(MScSideBars.Menu, open = OptionUtil.SomeBool.someFalse) )
      }
    }


    def render(): VdomElement = {
      val R = ScCssStatic.Menu.Rows

      // Основной пункт меню.
      val headlineChs = List[VdomElement](
        <.span(
          R.rowContent,
          crCtxProv.message( MsgCodes.`Settings` ),
        ),
        // Settings icon:
        Mui.SvgIcons.Settings {
          val css = new MuiSvgIconClasses {
            override val root = R.rightIcon.htmlClass
          }
          new MuiSvgIconProps {
            override val classes = css
          }
        }(),
      )

      MuiListItem {
        new MuiListItemProps {
          override val disableGutters = menuItemR.DISABLE_GUTTERS
          override val button         = true
          override val onClick        = _onClickCbF
          override val classes        = menuItemR.MENU_LIST_ITEM_CSS
        }
      } (
        scCssP.consume { scCss =>
          MuiListItemText {
            val css = new MuiListItemTextClasses {
              override val root = Css.flat (
                scCss.fgColor.htmlClass,
                R.rowLink.htmlClass,
              )
            }
            new MuiListItemTextProps {
              override val classes = css
            }
          } ( headlineChs: _* )
        },
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
