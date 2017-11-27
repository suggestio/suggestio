package io.suggest.sc.grid.v

import com.github.dantrain.react.stonecutter.{CSSGrid, GridComponents}
import diode.react.ModelProxy
import io.suggest.grid.build.{GridBuildArgs, GridBuildRes_t}
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdGridUtil, JdR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.grid.m.{GridBlockClick, HandleGridBuildRes, MGridS}
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.sc.tile.TileConstants
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.17 19:57
  * Description: Компонент ядра плитки.
  * Реализует несколько сложных концептов:
  * - элементы плитки должны быть html-тегами, а не компонентами
  * - карточки должны рендерится лениво, а не перерендериваться постоянно.
  *
  */
class GridCoreR(
                 jdGridUtil                 : JdGridUtil,
                 jdR                        : JdR
               ) {

  import MJdArgs.MJdArgsFastEq


  type Props = ModelProxy[MGridS]

  class Backend($: BackendScope[Props, Unit]) {

    /** Завершение обсчёта плитки. */
    private def onGridLayout(layoutRes: GridBuildRes_t): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCB($, HandleGridBuildRes(layoutRes))
    }
    private val _onGridLayoutF = ReactCommonUtil.cbFun1ToF( onGridLayout )

    /** Клик по карточке в плитке. */
    private def onBlockClick(nodeId: String): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GridBlockClick(nodeId))
    }


    def render(mgridProxy: Props): VdomElement = {
      val mgrid = mgridProxy.value

      CSSGrid {
        jdGridUtil.mkCssGridArgs(
          jds = jdGridUtil.jdTrees2bms(
            mgrid.ads
              .iterator
              .flatten
              .flatMap(_.flatGridTemplates)
          ),
          conf      = mgrid.jdConf,
          tagName   = GridComponents.DIV,
          gridBuildArgsF = { items =>
            GridBuildArgs(
              itemsExtDatas   = items,
              jdConf          = mgrid.jdConf,
              onLayout        = Some(_onGridLayoutF),
              offY            = TileConstants.CONTAINER_OFFSET_TOP
            )
          }
        )
      } {
        val iter = for {
          (ad, i) <- mgrid.ads.iterator
            .flatten
            .zipWithIndex
          rootId = ad.nodeId.getOrElse(i.toString)
          edges  = ad.flatGridEdges
          (tpl, j) <- ad.flatGridTemplates.iterator.zipWithIndex
        } yield {
          <.div(
            ^.key := (rootId + `.` + j),

            // Реакция на клики, когда nodeId задан.
            ad.nodeId.whenDefined { nodeId =>
              ^.onClick --> onBlockClick(nodeId)
            },

            mgridProxy.wrap { _ =>
              // Нельзя смешивать разные grid'ы, поэтому тут используем только внешний.
              MJdArgs(
                template = tpl,
                edges    = edges,
                jdCss    = mgrid.jdCss,
                conf     = mgrid.jdConf
              )
            } ( jdR.apply )
          )
        }
        iter.toVdomArray
      }
    }

  }


  val component = ScalaComponent.builder[Props]("GridCore")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(gridProxy: Props) = component(gridProxy)

}
