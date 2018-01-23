package io.suggest.sc.v.grid

import com.github.dantrain.react.stonecutter.{CSSGrid, GridComponents}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.grid.build._
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdGridUtil, JdR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.grid.{GridBlockClick, HandleGridBuildRes, MGridS}
import io.suggest.sc.tile.TileConstants
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.17 19:57
  * Description: Компонент ядра плитки.
  * Реализует несколько сложных концептов:
  * - элементы плитки должны быть html-тегами, а не компонентами
  * - карточки должны рендерится лениво, а не перерендериваться постоянно.
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
          gbArgs = MGridBuildArgsJs(
            itemsExtDatas = mgrid.ads
              .iterator
              .flatten
              .map { scAdData =>
                scAdData.focused.toOption.fold [IGbBlockPayload] {
                  // Несфокусированная карточка. Вернуть Left(blockMeta) с единственного стрипа.
                  MGbBlock(
                    scAdData.main.template.rootLabel.props1.bm.get
                  )
                } { foc =>
                  // Открытая карточка. Вернуть Right() со списком фокус-блоков:
                  //println( foc )
                  MGbSubItems(
                    jdGridUtil
                      .jdTrees2bms( foc.template.subForest )
                      .map { bm => MGbBlock(bm) }
                      .toList
                  )
                }
              }
              .toList,
            jdConf          = mgrid.jdConf,
            onLayout        = Some(_onGridLayoutF),
            offY            = TileConstants.CONTAINER_OFFSET_TOP
          ),
          conf      = mgrid.jdConf,
          tagName   = GridComponents.DIV
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
          // На телевизорах и прочих около-умных устройствах без нормальных устройств ввода,
          // для кликов подсвечиваются только ссылки.
          // Поэтому тут используется <A>-тег, хотя div был бы уместнее.
          <.a(
            ^.key := (rootId + `.` + j),

            // Реакция на клики, когда nodeId задан.
            ad.nodeId.whenDefined { nodeId =>
              ^.onClick --> onBlockClick(nodeId)
            },

            mgridProxy.wrap { _ =>
              // Нельзя одновременно использовать разные инстансы mgrid, поэтому для простоты и удобства используем только внешний.
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
