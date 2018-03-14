package io.suggest.sc.v

import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.hdr.{MenuOpenClose, SearchOpenClose, MHeaderStates}
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
import io.suggest.sc.m.menu.MMenuS
import io.suggest.sc.v.menu.MenuR
import japgolly.scalajs.react.raw.ReactNodeList

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 14:06
  * Description: Корневой react-компонент для выдачи третьего поколения.
  */
class ScRootR (
                protected[this] val scCssR      : ScCssR,
                val gridR                       : GridR,
                searchR                         : SearchR,
                protected[this] val headerR     : HeaderR,
                protected[this] val menuR       : MenuR,
                protected[this] val welcomeR    : WelcomeR,
                getScCssF                       : GetScCssF,
              ) {

  import MScCssArgs.MScCssArgsFastEq
  import MScSearch.MScSearchFastEq
  import gridR.GridPropsValFastEq
  import headerR.HeaderPropsValFastEq
  import welcomeR.WelcomeRPropsValFastEq
  import menuR.MenuRPropsValFastEq


  type Props = ModelProxy[MScRoot]

  protected[this] case class State(
                                    scCssArgsC     : ReactConnectProxy[IScCssArgs],
                                    gridPropsOptC  : ReactConnectProxy[gridR.PropsVal],
                                    headerPropsC   : ReactConnectProxy[Option[headerR.PropsVal]],
                                    wcPropsOptC    : ReactConnectProxy[Option[welcomeR.PropsVal]],
                                    searchC        : ReactConnectProxy[MScSearch],
                                    menuC          : ReactConnectProxy[menuR.PropsVal],
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
      s.searchC { searchPropsProxy =>
      s.menuC { menuPropsProxy =>
        val scCss = getScCssF()

        val INITIAL = scalacss.internal.Literal.Typed.initial.value

        val searchContentStyl = new StyleProps {
          override val zIndex    = scCss.Search.Z_INDEX
          override val overflowY = INITIAL
        }
        val contentStyl = new StyleProps {
          override val overflowY = INITIAL
        }

        val searchStyles = new SidebarStyles {
          override val sidebar = searchContentStyl
          override val content = contentStyl
        }

        Sidebar(
          new SidebarProps {
            override val sidebar      = menuR( menuPropsProxy ).rawNode
            override val pullRight    = false
            override val touch        = true
            override val transitions  = true
            override val docked       = menuPropsProxy.value.menuS.opened
            override val onSetOpen    = _onOpenMenuSidebarF
          }
        ) {
          // Содержимое правой панели (панель поиска)
          // search (правый) sidebar.
          Sidebar(
            new SidebarProps {
              override val sidebar      = searchR( searchPropsProxy ).rawNode
              override val onSetOpen    = _onOpenSearchSidebarF
              override val open         = searchPropsProxy.value.isShown
              override val transitions  = true
              override val touch        = true
              override val pullRight    = true
              override val styles       = searchStyles
            }
          )(
            <.div(
              // Рендер стилей перед снаружи и перед остальной выдачей.
              s.scCssArgsC { scCssR.apply },

              <.div(
                // Ссылаемся на стиль.
                scCss.Root.root,

                // Экран приветствия узла:
                s.wcPropsOptC { welcomeR.apply },

                // Компонент заголовка выдачи:
                s.headerPropsC { headerR.apply },

                // Рендер плитки карточек узла:
                s.gridPropsOptC { gridR.apply }

              )
            )
          )
        }
      }
      }
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
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
