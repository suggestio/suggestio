package io.suggest.sc.v.grid

import chandu0101.scalajs.react.components.materialui.{MuiCircularProgress, MuiCircularProgressClasses, MuiCircularProgressProps, MuiColorTypes}
import diode.FastEq
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.grid.{GridConst, GridScrollUtil}
import io.suggest.jd.render.v._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.grid.{GridScroll, MGridCoreS, MGridS, MScAdData}
import io.suggest.sc.styl.{GetScCssF, ScCssStatic}
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
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
             gridCoreR                  : GridCoreR,
             jdCssR                     : JdCssR,
             getScCssF                  : GetScCssF
           ) {


  type Props_t = MGridS
  type Props = ModelProxy[Props_t]


  /** Модель состояния компонента. */
  protected[this] case class State(
                                    jdCssC              : ReactConnectProxy[JdCss],
                                    coreC               : ReactConnectProxy[MGridCoreS],
                                    gridSzC             : ReactConnectProxy[MSize2di],
                                    loaderPotC          : ReactConnectProxy[Pot[Vector[MScAdData]]],
                                  )

  class Backend($: BackendScope[Props, State]) {

    /** Скроллинг плитки. */
    private def onGridScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GridScroll(scrollTop))
    }

    def render(s: State): VdomElement = {
      val ScCss = getScCssF()
      val GridCss = ScCss.Grid
      val smFlex: TagMod = ScCssStatic.smFlex

      <.div(
        smFlex, GridCss.outer,

        <.div(
          smFlex, GridCss.wrapper,
          ^.id := GridScrollUtil.SCROLL_CONTAINER_ID,
          ^.onScroll ==> onGridScroll,

          <.div(
            GridCss.content,

            // Рендер style-тега.
            s.jdCssC { jdCssR.apply },

            // Начинается [пере]сборка всей плитки
            // TODO Функция сборки плитки неоптимальна и перегенеривается на каждый чих. Это вызывает лишние перерендеры контейнера плитки.
            {
              // Непосредственный рендер плитки, снаружи от рендера connect-зависимого контейнера плитки.
              val gridCore = s.coreC { gridCoreR.apply }

              s.gridSzC { gridSzProxy =>
                val gridSz = gridSzProxy.value

                <.div(
                  ScCssStatic.Grid.container,
                  GridCss.container,
                  ^.width  := gridSz.width.px,
                  ^.height := (gridSz.height + GridConst.CONTAINER_OFFSET_BOTTOM + GridConst.CONTAINER_OFFSET_TOP).px,

                  gridCore
                )
              }
            },

            // Крутилка подгрузки карточек.
            s.loaderPotC { adsPotProxy =>
              ReactCommonUtil.maybeEl( adsPotProxy.value.isPending ) {
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


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      // Наконец, сборка самого состояния.
      State(
        jdCssC = propsProxy.connect { props =>
          props.core.jdCss
        }( JdCss.JdCssFastEq ),

        coreC = propsProxy.connect { props =>
          props.core
        }( MGridCoreS.MGridCoreSFastEq ),

        gridSzC = propsProxy.connect { props =>
          props.core.gridBuild.gridWh
        }( FastEq.ValueEq ),

        loaderPotC = propsProxy.connect { props =>
          props.core.ads
        }( FastEqUtil.PotIsPendingFastEq ),

      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
