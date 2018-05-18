package io.suggest.sc.v

import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.hdr.{MHeaderStates, MenuOpenClose, SearchOpenClose}
import io.suggest.sc.m.search.MScSearch
import io.suggest.sc.styl.{GetScCssF, IScCssArgs, MScCssArgs}
import io.suggest.sc.v.grid.GridR
import io.suggest.sc.v.hdr.HeaderR
import io.suggest.sc.v.inx.WelcomeR
import io.suggest.sc.v.search.SearchR
import io.suggest.spa.OptFastEq.Wrapped
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.{ReactCommonUtil, StyleProps}
import io.suggest.sc.v.menu.{AboutSioR, EditAdR, EnterLkRowR, MenuR}
import io.suggest.spa.OptFastEq
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 14:06
  * Description: Корневой react-компонент для выдачи третьего поколения.
  */
class ScRootR (
                val scCssR      : ScCssR,
                val gridR       : GridR,
                searchR         : SearchR,
                val headerR     : HeaderR,
                val menuR       : MenuR,
                val enterLkRowR : EnterLkRowR,
                val editAdR     : EditAdR,
                val aboutSioR   : AboutSioR,
                val welcomeR    : WelcomeR,
                getScCssF       : GetScCssF,
              ) {

  import MScCssArgs.MScCssArgsFastEq
  import MScSearch.MScSearchFastEq
  import gridR.GridPropsValFastEq
  import headerR.HeaderPropsValFastEq
  import menuR.MenuRPropsValFastEq
  import enterLkRowR.EnterLkRowRPropsValFastEq
  import editAdR.EditAdRPropsValFastEq
  import aboutSioR.AboutSioRPropsValFastEq
  import welcomeR.WelcomeRPropsValFastEq


  type Props = ModelProxy[MScRoot]

  protected[this] case class State(
                                    scCssArgsC          : ReactConnectProxy[IScCssArgs],
                                    gridPropsOptC       : ReactConnectProxy[gridR.PropsVal],
                                    headerPropsC        : ReactConnectProxy[Option[headerR.PropsVal]],
                                    wcPropsOptC         : ReactConnectProxy[Option[welcomeR.PropsVal]],
                                    enterLkRowC         : ReactConnectProxy[Option[enterLkRowR.PropsVal]],
                                    editAdC             : ReactConnectProxy[Option[editAdR.PropsVal]],
                                    aboutSioC           : ReactConnectProxy[Option[aboutSioR.PropsVal]],
                                    searchC             : ReactConnectProxy[MScSearch],
                                    searchOpenedSomeC   : ReactConnectProxy[Some[Boolean]],
                                    menuC               : ReactConnectProxy[menuR.PropsVal],
                                    menuOpenedSomeC     : ReactConnectProxy[Some[Boolean]]
                                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onOpenSearchSidebar(opened: Boolean): Callback = {
      dispatchOnProxyScopeCB( $, SearchOpenClose(opened) )
    }
    private val _onOpenSearchSidebarF = ReactCommonUtil.cbFun1ToJsCb( _onOpenSearchSidebar )


    private def _onOpenMenuSidebar(opened: Boolean): Callback = {
      dispatchOnProxyScopeCB( $, MenuOpenClose(opened) )
    }
    private val _onOpenMenuSidebarF = ReactCommonUtil.cbFun1ToJsCb( _onOpenMenuSidebar )


    def render(s: State): VdomElement = {
      val scCss = getScCssF()

      // Рендерим всё линейно, а не деревом, чтобы избежать вложенных connect.apply-фунцкий и сопутствующих эффектов.
      // Содержимое правой панели (панель поиска)
      // search (правый) sidebar.
      val gridBody = <.div(
        // Ссылаемся на стиль.
        scCss.Root.root,

        // Экран приветствия узла:
        s.wcPropsOptC { welcomeR.apply },

        // Компонент заголовка выдачи:
        s.headerPropsC { headerR.apply },

        // Рендер плитки карточек узла:
        s.gridPropsOptC { gridR.apply }
      )

      val searchBarBody = s.searchC { searchR.apply }

      val menuSideBarBodyInner = <.div(
        // Строка входа в личный кабинет
        s.enterLkRowC { enterLkRowR.apply },

        // Кнопка редактирования карточки.
        s.editAdC { editAdR.apply },

        // Рендер кнопк
        s.aboutSioC { aboutSioR.apply }
      )
      val menuSideBarBody = s.menuC { menuPropsProxy =>
        menuR( menuPropsProxy )( menuSideBarBodyInner )
      }

      val css_initial = scalacss.internal.Literal.Typed.initial.value

      val searchContentStyl = new StyleProps {
        override val zIndex    = scCss.Search.Z_INDEX
        override val overflowY = css_initial
      }
      val contentStyl = new StyleProps {
        override val overflowY = css_initial
      }
      val overlayStyl = new StyleProps {
        override val zIndex = 3
      }

      val searchStyles = new SidebarStyles {
        override val sidebar = searchContentStyl
        override val content = contentStyl
        override val overlay = overlayStyl
      }

      val searchSideBar = s.searchOpenedSomeC { searchOpenedSomeProxy =>
        Sidebar(
          new SidebarProps {
            override val sidebar      = searchBarBody.rawNode
            override val onSetOpen    = _onOpenSearchSidebarF
            override val open         = searchOpenedSomeProxy.value.value
            override val transitions  = true
            override val touch        = true
            override val pullRight    = true
            override val styles       = searchStyles
          }
        )( gridBody )
      }

      val menuSideBar = s.menuOpenedSomeC { menuOpenedSomeProxy =>
        Sidebar(
          new SidebarProps {
            override val sidebar      = menuSideBarBody.rawNode
            override val pullRight    = false
            override val touch        = true
            override val transitions  = true
            override val docked       = menuOpenedSomeProxy.value.value
            override val onSetOpen    = _onOpenMenuSidebarF
          }
        )( searchSideBar )
      }

      <.div(
        // Рендер стилей перед снаружи и перед остальной выдачей.
        s.scCssArgsC { scCssR.apply },

        menuSideBar
      )

    }

  }


  val component = ScalaComponent.builder[Props]("Root")
    .initialStateFromProps { propsProxy =>
      State(
        scCssArgsC  = propsProxy.connect(_.index.scCss.args),

        gridPropsOptC = propsProxy.connect { mroot =>
          gridR.PropsVal(
            grid    = mroot.grid,
            fgColor = mroot.index.resp.toOption.flatMap(_.colors.fg)
          )
        },

        headerPropsC = propsProxy.connect { props =>
          for {
            resp <- props.index.resp.toOption
          } yield {
            headerR.PropsVal(
              hdrState  = if (props.index.search.isShown) {
                MHeaderStates.Search
              } else if (props.index.menu.opened) {
                MHeaderStates.Menu
              } else {
                MHeaderStates.PlainGrid
              },
              node      = resp
            )
          }
        },

        wcPropsOptC = propsProxy.connect { props =>
          for {
            resp    <- props.index.resp.toOption
            wcInfo  <- resp.welcome
            wcState <- props.index.welcome
          } yield {
            welcomeR.PropsVal(
              wcInfo   = wcInfo,
              nodeName = resp.name,
              state    = wcState
            )
          }
        },

        searchC = propsProxy.connect(_.index.search),

        menuC = propsProxy.connect { props =>
          menuR.PropsVal(
            menuS = props.index.menu
          )
        },

        enterLkRowC = propsProxy.connect { props =>
          for {
            scJsRouter <- props.internals.jsRouter.toOption
          } yield {
            enterLkRowR.PropsVal(
              isLoggedIn      = props.internals.conf.isLoggedIn,
              // TODO А если текущий узел внутри карточки, то что тогда? Надо как-то по adn-типу фильтровать.
              isMyAdnNodeId   = props.index.state
                .currRcvrId
                .filter { _ =>
                  props.index.resp.exists(_.isMyNode)
                },
              scJsRouter = scJsRouter
            )
          }
        },

        editAdC = propsProxy.connect { props =>
          for {
            scJsRouter      <- props.internals.jsRouter.toOption
            focusedAdOuter  <- props.grid.core.focusedAdOpt
            focusedData     <- focusedAdOuter.focused.toOption
            if focusedData.canEdit
            focusedAdId     <- focusedAdOuter.nodeId
          } yield {
            editAdR.PropsVal(
              adId      = focusedAdId,
              scRoutes  = scJsRouter
            )
          }
        },

        aboutSioC = propsProxy.connect { props =>
          val propsVal = aboutSioR.PropsVal(
            aboutNodeId = props.internals.conf.aboutSioNodeId
          )
          Some(propsVal)
        },

        menuOpenedSomeC = propsProxy.connect { props =>
          Some( props.index.menu.opened )
        }( OptFastEq.OptValueEq ),

        searchOpenedSomeC = propsProxy.connect { props =>
          Some( props.index.search.isShown )
        }( OptFastEq.OptValueEq )

      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
