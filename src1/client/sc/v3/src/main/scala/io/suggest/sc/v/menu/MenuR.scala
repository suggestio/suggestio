package io.suggest.sc.v.menu

import com.materialui.{MuiDivider, MuiDrawerAnchor, MuiList, MuiListItem, MuiSwipeableDrawer}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.v.dia.dlapp.DlAppMenuItemR
import io.suggest.sc.v.dia.settings.SettingsMenuItemR
import io.suggest.sc.v.hdr.LeftR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

import scala.scalajs.js

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
             scNodesMenuItemR         : ScNodesMenuItemR,
             indexesRecentR           : IndexesRecentR,
             logOutR                  : LogOutR,
             scCssP                   : React.Context[ScCss],
           ) {

  type Props = ModelProxy[MScRoot]

  case class State(
                    menuOpenedSomeC           : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private def _onSetOpen(opened: Boolean): Callback =
      dispatchOnProxyScopeCB( $, SideBarOpenClose(MScSideBars.Menu, OptionUtil.SomeBool(opened)) )
    //private val _onSetOpenMenuSidebarF = ReactCommonUtil.cbFun1ToJsCb( _onSetOpen )

    private val _onOpenCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      _onSetOpen( true )
    }
    private val _onCloseCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      _onSetOpen( false )
    }

    def render(propsProxy: Props, s: State): VdomElement = {
      // Сборка панели меню:
      val menuRows = <.div(
        ScCssStatic.Menu.Rows.rowsContainer,

        MuiList()(

          // Список недавних узлов.
          indexesRecentR.component( propsProxy ),

          MuiListItem()(
            MuiDivider()(),
          ),

          // Строка входа в личный кабинет. В кордове - скрыта.
          enterLkRowR.component( propsProxy ),

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
          scNodesMenuItemR.component( propsProxy ),

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

      s.menuOpenedSomeC { menuOpenedSomeProxy =>
        MuiSwipeableDrawer(
          new MuiSwipeableDrawer.Props {
            override val onOpen = _onOpenCbF
            override val open = menuOpenedSomeProxy.value.value
            override val onClose = _onCloseCbF
            override val anchor = MuiDrawerAnchor.left
            // TODO Тут при iOS обычно отключают это. Возможно, тоже нужен iOS+browser-вариант с отключением левого свайпа?
            override val disableSwipeToOpen = false
            override val transitionDuration = {
              if (propsProxy.value.dev.platform.isUsingNow) {
                // default - анимация по умолчанию.
                js.undefined
              } else {
                // Выключить анимацию, если приложение скрыто или экран выключен.
                js.defined( 0d )
              }
            }
          }
        )(
          menuSideBarBody,
        )
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
    .renderBackend[Backend]
    .build

}
