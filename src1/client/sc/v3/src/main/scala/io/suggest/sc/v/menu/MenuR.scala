package io.suggest.sc.v.menu

import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import com.materialui.{MuiDivider, MuiList, MuiListItem}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.{ReactCommonUtil, StyleProps}
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.v.dia.dlapp.DlAppMenuItemR
import io.suggest.sc.v.dia.settings.SettingsMenuItemR
import io.suggest.sc.v.hdr.LeftR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 21:44
  * Description: Главный компонент левой панели меню выдачи.
  */
class MenuR(
             enterLkRowR              : EnterLkRowR,
             editAdR                  : EditAdR,
             aboutSioR                : AboutSioR,
             dlAppMenuItemR           : DlAppMenuItemR,
             settingsMenuItemR        : SettingsMenuItemR,
             leftR                    : LeftR,
             versionR                 : VersionR,
             scNodesBtnR              : ScNodesBtnR,
             indexesRecentR           : IndexesRecentR,
             logOutR                  : LogOutR,
             scCssP                   : React.Context[ScCss],
           ) {

  type Props = ModelProxy[MScRoot]

  case class State(
                    menuOpenedSomeC           : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private val _onSetOpenMenuSidebarF = ReactCommonUtil.cbFun1ToJsCb { opened: Boolean =>
      dispatchOnProxyScopeCB( $, SideBarOpenClose(MScSideBars.Menu, OptionUtil.SomeBool(opened)) )
    }

    def render(propsProxy: Props, s: State, propsChildren: PropsChildren): VdomElement = {
      // Сборка панели меню:
      val menuRows = <.div(
        ScCssStatic.Menu.Rows.rowsContainer,

        MuiList()(

          // Список недавних узлов.
          indexesRecentR.component( propsProxy ),

          MuiListItem()(
            MuiDivider()
          ),

          // Строка входа в личный кабинет. В кордове - скрыта.
          ReactCommonUtil.maybeNode( propsProxy.value.dev.platform.isBrowser )(
            enterLkRowR.component( propsProxy )
          ),

          // Кнопка редактирования карточки.
          editAdR.component( propsProxy ),

          // Рендер кнопки "О проекте"
          propsProxy.wrap( _.internals.conf.aboutSioNodeId )( aboutSioR.component.apply ),

          // Пункт скачивания мобильного приложения.
          ReactCommonUtil.maybeNode( propsProxy.value.dev.platform.isDlAppAvail ) {
            dlAppMenuItemR.component( propsProxy )
          },

          // Пункт открытия диалога с настройками выдачи.
          settingsMenuItemR.component( propsProxy ),

          // Доступ к диалогу управления узлами.
          scNodesBtnR.component( propsProxy ),

          // Кнопка разлогинивания.
          logOutR.component( propsProxy ),

        ),
      )

      val vsn = versionR.component(): VdomElement

      // Кнопка сокрытия панели влево
      val hideBtn = propsProxy.wrap(_ => None)( leftR.apply ): VdomElement

      val panelCommon = ScCssStatic.Root.panelCommon: TagMod
      val panelBg = ScCssStatic.Root.panelBg: TagMod

      val menuSideBarBody = scCssP.consume { scCss =>
        val menuCss = scCss.Menu

        <.div(
          panelCommon,
          menuCss.panel,

          // Фон панели.
          <.div(
            panelBg,
            scCss.panelBg,
          ),

          // Контейнер для непосредственного контента панели.
          <.div(
            menuCss.content,

            hideBtn,

            // Менюшка
            menuRows,
          ),

          vsn,
        )    // .panel
      }

      val menuSideBarStyles = {
        val zIndexBase = ScCss.sideBarZIndex * 2
        val sidebarStyl = new StyleProps {
          override val zIndex    = zIndexBase
          override val overflowY = ScCss.css_initial
        }
        val overlayStyl = new StyleProps {
          override val zIndex = zIndexBase - 1
        }
        val contentStyl = new StyleProps {
          override val overflowY = ScCss.css_initial
        }
        new SidebarStyles {
          override val sidebar = sidebarStyl
          override val overlay = overlayStyl
          override val content = contentStyl
        }
      }

      s.menuOpenedSomeC { menuOpenedSomeProxy =>
        Sidebar(
          new SidebarProps {
            override val sidebar      = menuSideBarBody.rawNode
            override val pullRight    = false
            override val touch        = true
            override val transitions  = true
            override val open         = menuOpenedSomeProxy.value.value
            override val onSetOpen    = _onSetOpenMenuSidebarF
            override val shadow       = true
            override val styles       = menuSideBarStyles
          }
        )( propsChildren )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        menuOpenedSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.index.menu.opened )
        }( FastEq.AnyRefEq ),

      )
    }
    .renderBackendWithChildren[Backend]
    .build

}
