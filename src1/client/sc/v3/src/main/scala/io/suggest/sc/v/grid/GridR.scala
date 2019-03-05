package io.suggest.sc.v.grid

import chandu0101.scalajs.react.components.materialui.{MuiCircularProgress, MuiCircularProgressClasses, MuiCircularProgressProps, MuiColorTypes}
import diode.FastEq
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.grid.{GridConst, GridScrollUtil}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.m.grid.{GridScroll, MGridS, MScAdData}
import io.suggest.sc.styl.ScCssStatic
import io.suggest.spa.FastEqUtil
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
             scReactCtxP                : React.Context[MScReactCtx],
           ) {


  type Props_t = MGridS
  type Props = ModelProxy[Props_t]


  /** Модель состояния компонента. */
  protected[this] case class State(
                                    gridSzC             : ReactConnectProxy[MSize2di],
                                    loaderPotC          : ReactConnectProxy[Pot[Vector[MScAdData]]],
                                  )

  class Backend($: BackendScope[Props, State]) {

    /** Скроллинг плитки. */
    private def onGridScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GridScroll(scrollTop))
    }

    def render(p: Props, s: State, children: PropsChildren): VdomElement = {
      scReactCtxP.consume { scReactCtx =>
        val ScCss = scReactCtx.scCss
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

                    children,
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

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      // Наконец, сборка самого состояния.
      State(
        gridSzC = propsProxy.connect { props =>
          props.core.gridBuild.gridWh
        }( FastEq.ValueEq ),

        loaderPotC = propsProxy.connect { props =>
          props.core.ads
        }( FastEqUtil.PotIsPendingFastEq ),

      )
    }
    .renderBackendWithChildren[Backend]
    .build

  def apply(props: Props)(children: VdomNode*) = component(props)(children: _*)

}
