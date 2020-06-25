package io.suggest.sc.v.grid

import com.materialui.{MuiCircularProgress, MuiCircularProgressClasses, MuiCircularProgressProps, MuiColorTypes}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.css.CssR
import io.suggest.grid.{GridConst, GridScrollUtil}
import io.suggest.jd.render.v.{JdCss, JdCssStatic}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.m.grid.{GridScroll, MGridCoreS, MGridS}
import io.suggest.sc.v.styl.ScCssStatic
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 14:38
  * Description: React-компонент плитки карточек.
  */
class GridR(
             jdCssStatic                : JdCssStatic,
             val gridCoreR              : GridCoreR,
             scReactCtxP                : React.Context[MScReactCtx],
           ) {


  type Props_t = MGridS
  type Props = ModelProxy[Props_t]


  /** Модель состояния компонента. */
  protected[this] case class State(
                                    jdCssC              : ReactConnectProxy[JdCss],
                                    gridSzC             : ReactConnectProxy[MSize2di],
                                    gridCoreC           : ReactConnectProxy[gridCoreR.Props_t],
                                    loaderPotC          : ReactConnectProxy[Some[Boolean]],
                                  )

  class Backend($: BackendScope[Props, State]) {

    /** Скроллинг плитки. */
    private def onGridScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GridScroll(scrollTop))
    }

    def render(p: Props, s: State): VdomElement = {
      // Рендер jd-css:
      val jdCssStatic1 = p.wrap(_ => jdCssStatic)( CssR.compProxied.apply )
      val jdCss1 = s.jdCssC { CssR.compProxied.apply }

      // Непосредственный рендер плитки - коннекшен в отдельный компонент, снаружи от рендера connect-зависимого контейнера плитки.
      val gridCore = s.gridCoreC { gridCoreR.apply }

      val smFlex = ScCssStatic.smFlex: TagMod

      scReactCtxP.consume { scReactCtx =>
        val ScCss = scReactCtx.scCss
        val GridCss = ScCss.Grid

        <.div(
          smFlex, GridCss.outer, ScCss.bgColor,

          <.div(
            smFlex, GridCss.wrapper,
            ^.id := GridScrollUtil.SCROLL_CONTAINER_ID,
            ^.onScroll ==> onGridScroll,

            <.div(
              GridCss.content,

              // Начинается [пере]сборка всей плитки
              // TODO Функция сборки плитки неоптимальна и перегенеривается на каждый чих. Это вызывает лишние перерендеры контейнера плитки.
              {
                val stylesTm = TagMod(
                  ScCssStatic.Grid.container,
                  GridCss.container,
                )
                s.gridSzC { gridSzProxy =>
                  val gridSz = gridSzProxy.value
                  <.div(
                    stylesTm,
                    ^.width  := gridSz.width.px,
                    ^.height := (gridSz.height + GridConst.CONTAINER_OFFSET_BOTTOM + GridConst.CONTAINER_OFFSET_TOP).px,

                    jdCssStatic1,
                    jdCss1,
                    gridCore,
                  )
                }
              },

              // Крутилка подгрузки карточек.
              s.loaderPotC { adsPotPendingSomeProxy =>
                ReactCommonUtil.maybeEl( adsPotPendingSomeProxy.value.value ) {
                  MuiCircularProgress {
                    val cssClasses = new MuiCircularProgressClasses {
                      override val root = ScCss.Grid.loader.htmlClass
                    }
                    new MuiCircularProgressProps {
                      override val color   = MuiColorTypes.secondary
                      override val classes = cssClasses
                    }
                  }
                }
              }

            )
          )
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      // Наконец, сборка самого состояния.
      State(
        jdCssC = propsProxy.connect(_.core.jdRuntime.jdCss)( JdCss.JdCssFastEq ),

        gridSzC = propsProxy.connect { props =>
          props.core.gridBuild.gridWh
        }( FastEq.ValueEq ),

        gridCoreC = propsProxy.connect(_.core)( MGridCoreS.MGridCoreSFastEq ),

        loaderPotC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.core.ads.isPending )
        }( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

}
