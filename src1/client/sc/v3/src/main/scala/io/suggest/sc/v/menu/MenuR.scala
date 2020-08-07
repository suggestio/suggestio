package io.suggest.sc.v.menu

import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import com.materialui.{MuiDivider, MuiList, MuiListItem}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.proto.http.HttpConst
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.{ReactCommonUtil, StyleProps}
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.v.dia.dlapp.DlAppMenuItemR
import io.suggest.sc.v.dia.settings.SettingsMenuItemR
import io.suggest.sc.v.hdr.LeftR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 21:44
  * Description: Главный компонент левой панели меню выдачи.
  */
class MenuR(
             val enterLkRowR          : EnterLkRowR,
             val editAdR              : EditAdR,
             val aboutSioR            : AboutSioR,
             dlAppMenuItemR           : DlAppMenuItemR,
             settingsMenuItemR        : SettingsMenuItemR,
             leftR                    : LeftR,
             versionR                 : VersionR,
             indexesRecentR           : IndexesRecentR,
             scCssP                   : React.Context[ScCss],
           ) {

  type Props = ModelProxy[MScRoot]

  case class State(
                    enterLkRowC               : ReactConnectProxy[Option[enterLkRowR.PropsVal]],
                    editAdC                   : ReactConnectProxy[Option[editAdR.PropsVal]],
                    aboutSioC                 : ReactConnectProxy[aboutSioR.Props_t],
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

          // Строка входа в личный кабинет
          s.enterLkRowC { enterLkRowR.apply },

          // Кнопка редактирования карточки.
          s.editAdC { editAdR.apply },

          // Рендер кнопки "О проекте"
          s.aboutSioC { aboutSioR.apply },

          // Пункт скачивания мобильного приложения.
          ReactCommonUtil.maybeNode( propsProxy.value.dev.platform.isDlAppAvail ) {
            dlAppMenuItemR.component( propsProxy )
          },

          // Пункт открытия диалога с настройками выдачи.
          settingsMenuItemR.component( propsProxy ),

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

        enterLkRowC = propsProxy.connect { props =>
          for {
            scJsRouter <- props.internals.jsRouter.jsRouter.toOption
            plat = props.dev.platform
          } yield {
            enterLkRowR.PropsVal(
              isLoggedIn      = props.index.respOpt.flatMap(_.isLoggedIn) contains true,
              // TODO А если текущий узел внутри карточки, то что тогда? Надо как-то по adn-типу фильтровать.
              isMyAdnNodeId   = props.index.state
                .rcvrId
                .filter { _ =>
                  props.index.resp.exists(_.isMyNode contains true)
                },
              scJsRouter = scJsRouter,
              canInternalLogin = {
                plat.isCordova ||
                (dom.window.location.protocol startsWith HttpConst.Proto.HTTPS) ||
                scalajs.LinkingInfo.developmentMode
              }
            )
          }
        }( OptFastEq.Wrapped(enterLkRowR.EnterLkRowRPropsValFastEq) ),

        editAdC = propsProxy.connect { props =>
          for {
            scJsRouter      <- props.internals.jsRouter.jsRouter.toOption
            focusedAdOuter  <- props.grid.core.focusedAdOpt
            focusedData     <- focusedAdOuter.focused.toOption
            if focusedData.info.canEdit
            focusedAdId     <- focusedAdOuter.nodeId
          } yield {
            editAdR.PropsVal(
              adId      = focusedAdId,
              scRoutes  = scJsRouter
            )
          }
        }( OptFastEq.Wrapped(editAdR.EditAdRPropsValFastEq) ),

        aboutSioC = propsProxy.connect { props =>
          val propsVal = aboutSioR.PropsVal(
            aboutNodeId = props.internals.conf.aboutSioNodeId
          )
          Option(propsVal)
        }( OptFastEq.Wrapped(aboutSioR.AboutSioRPropsValFastEq) ),

        menuOpenedSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.index.menu.opened )
        }( FastEq.AnyRefEq ),

      )
    }
    .renderBackendWithChildren[Backend]
    .build

}
