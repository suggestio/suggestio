package io.suggest.sc.grid.v

import com.github.dantrain.react.stonecutter.CSSGrid
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.blk.BlockPaddings
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.{MJdArgs, MJdCssArgs, MJdRenderArgs}
import io.suggest.jd.render.v.{JdCss, JdCssR, JdGridUtil, JdR}
import io.suggest.sc.grid.m.MGridS
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.spa.OptFastEq

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 14:38
  * Description: React-компонент плитки карточек.
  */
class GridR(
             jdGridUtil                 : JdGridUtil,
             jdR                        : JdR,
             jdCssR                     : JdCssR,
             getScCssF                  : GetScCssF
           ) {

  import MJdConf.MJdConfFastEq

  /** Модель пропертисов компонента плитки.
    *
    * param grid Состояние плитки.
    * param screen Состояние экрана.
    */
  /*
  case class PropsVal(
                       grid   : MGridS,
                       screen : MScreen
                     )
  implicit object GridPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.grid ===* b.grid) &&
        (a.screen ===* b.screen)
    }
  }
  */

  /** Модель состояния компонента.
    *
    * @param jdConfOptC Полуготовый инстанс MJdArgs без конкретных эджей и шаблона внутри.
    */
  protected[this] case class State(
                                    jdConfOptC    : ReactConnectProxy[Option[MJdConf]]
                                  )

  type Props = ModelProxy[Option[MGridS]]

  class Backend($: BackendScope[Props, State]) {

    def render(gridStateOptProxy: Props, s: State): VdomElement = {
      val ScCss = getScCssF()
      val GridCss = ScCss.Grid

      gridStateOptProxy.value.whenDefinedEl { gridState =>
        <.div(
          ScCss.smFlex, GridCss.outer,

          <.div(
            ScCss.smFlex, GridCss.wrapper,

            <.div(
              GridCss.content,

              s.jdConfOptC { jdConfOptProxy =>
                jdConfOptProxy.value.whenDefinedEl { jdConf =>
                  val templates = gridState.ads.map(_.template)

                  val jdCss = JdCss(
                    MJdCssArgs(
                      templates = templates,
                      conf = jdConf
                    )
                  )

                  // Начинается [пере]сборка всей плитки
                  <.div(
                    GridCss.container,

                    // Рендер style-тега.
                    jdConfOptProxy.wrap(_ => jdCss)(jdCssR.apply),

                    // Наконец сама плитка карточек:
                    CSSGrid {
                      jdGridUtil.mkCssGridArgs(
                        jds  = templates,
                        conf = jdConf
                      )
                    } (
                      gridState.ads
                        .iterator
                        .zipWithIndex
                        .map { case (scAdData, i) =>
                          jdConfOptProxy.wrap { _ =>

                            MJdArgs(
                              template    = scAdData.template,
                              renderArgs  = MJdRenderArgs(
                                edges     = scAdData.edges
                              ),
                              jdCss       = jdCss,
                              conf        = jdConf
                            )
                          } { jdArgsProxy =>
                            <.div(
                              ^.key := i.toString,
                              jdR( jdArgsProxy)
                            )
                          }
                        }
                        .toVdomArray
                    )
                  )
                }
              }

            )
          )
        )
      }
    }

  }

  val component = ScalaComponent.builder[Props]("Grid")
    .initialStateFromProps { gridStateOptProxy =>
      State(
        jdConfOptC = gridStateOptProxy.connect { gridStateOpt =>
          for (gridState <- gridStateOpt) yield {
            MJdConf(
              isEdit            = false,
              szMult            = gridState.szMult,
              blockPadding      = BlockPaddings.default,
              gridColumnsCount  = gridState.columnCount
            )
          }
        }( OptFastEq.Wrapped )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
