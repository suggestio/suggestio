package io.suggest.sc.v.menu

import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import com.materialui.MuiList
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.{ReactCommonUtil, StyleProps}
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.{MScReactCtx, MScRoot}
import io.suggest.sc.v.dia.dlapp.DlAppMenuItemR
import io.suggest.sc.v.dia.settings.SettingsMenuItemR
import io.suggest.sc.v.hdr.LeftR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import io.suggest.ueq.UnivEqUtil._

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
             scReactCtxP              : React.Context[MScReactCtx],
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
          // Строка входа в личный кабинет
          s.enterLkRowC { enterLkRowR.apply },

          // Кнопка редактирования карточки.
          s.editAdC { editAdR.apply },

          // Рендер кнопки "О проекте"
          s.aboutSioC { aboutSioR.apply },

          // Пункт скачивания мобильного приложения.
          dlAppMenuItemR( propsProxy ),

          // Пункт открытия диалога с настройками выдачи.
          settingsMenuItemR.component( propsProxy ),

        ),
      )

      val vsn = versionR.component(): VdomElement

      // Кнопка сокрытия панели влево
      val hideBtn = propsProxy.wrap(_ => None)( leftR.apply ): VdomElement

      val panelCommon = ScCssStatic.Root.panelCommon: TagMod
      val panelBg = ScCssStatic.Root.panelBg: TagMod

      val menuSideBarBody = scReactCtxP.consume { scReactCtx =>
        val menuCss = scReactCtx.scCss.Menu

        <.div(
          panelCommon,
          menuCss.panel,

          // Фон панели.
          <.div(
            panelBg,
            scReactCtx.scCss.panelBg,
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
            if props.dev.platform.isBrowser
          } yield {
            enterLkRowR.PropsVal(
              isLoggedIn      = props.internals.conf.isLoggedIn,
              // TODO А если текущий узел внутри карточки, то что тогда? Надо как-то по adn-типу фильтровать.
              isMyAdnNodeId   = props.index.state
                .rcvrId
                .filter { _ =>
                  props.index.resp.exists(_.isMyNode contains true)
                },
              scJsRouter = scJsRouter
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
