package io.suggest.sc.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.grid.v.GridR
import io.suggest.sc.hdr.m.MHeaderStates
import io.suggest.sc.hdr.v.HeaderR
import io.suggest.sc.inx.m.MScIndex
import io.suggest.sc.inx.v.wc.WelcomeR
import io.suggest.sc.m.MScRoot
import io.suggest.sc.search.m.MScSearch
import io.suggest.sc.search.v.SearchR
import io.suggest.sc.styl.{GetScCssF, MScCssArgs}
import io.suggest.spa.OptFastEq.Wrapped
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

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
                protected[this] val welcomeR    : WelcomeR,
                getScCssF                       : GetScCssF,
              ) {

  import MScCssArgs.MScCssArgsFastEq
  import MScIndex.MScIndexFastEq
  import MScSearch.MScSearchFastEq
  import gridR.GridPropsValFastEq
  import headerR.HeaderPropsValFastEq
  import welcomeR.WelcomeRPropsValFastEq


  type Props = ModelProxy[MScRoot]

  protected[this] case class State(
                                    scCssArgsC     : ReactConnectProxy[MScCssArgs],
                                    indexPropsC    : ReactConnectProxy[MScIndex],
                                    gridPropsOptC  : ReactConnectProxy[gridR.PropsVal],
                                    headerPropsC   : ReactConnectProxy[Option[headerR.PropsVal]],
                                    wcPropsOptC    : ReactConnectProxy[Option[welcomeR.PropsVal]],
                                    searchC        : ReactConnectProxy[MScSearch]
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      val scCss = getScCssF()
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

          // Правая панель (поиск)
          s.searchC { searchR.apply },

          // Рендер плитки карточек узла:
          s.gridPropsOptC { gridR.apply }

        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]("Root")
    .initialStateFromProps { propsProxy =>
      State(
        scCssArgsC  = propsProxy.connect(_.scCssArgs),
        indexPropsC = propsProxy.connect(_.index),

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
              // TODO Определять маркер состояния на основе состояния полей в props.
              hdrState  = if (props.index.search.isShown) {
                MHeaderStates.Search
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

        searchC = propsProxy.connect(_.index.search)

      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
