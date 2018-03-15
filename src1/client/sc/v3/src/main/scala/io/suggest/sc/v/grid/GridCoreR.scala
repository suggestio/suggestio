package io.suggest.sc.v.grid

import com.github.dantrain.react.stonecutter.{CSSGrid, GridComponents}
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.grid.build._
import io.suggest.jd.render.m.{MJdArgs, MJdRenderArgs}
import io.suggest.jd.render.v.{JdGridUtil, JdR}
import io.suggest.jd.tags.JdTag
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.grid._
import io.suggest.sc.tile.TileConstants
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}

import scalaz.Tree

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

    /** Конвертация одной карточки в один блок для рендера в плитке. */
    private def __blockRenderData2GbPayload(stripTpl: Tree[JdTag], brd: MBlkRenderData): MGbBlock = {
      // Несфокусированная карточка. Вернуть blockMeta с единственного стрипа.
      val stripJdt = stripTpl.rootLabel
      val bm = stripJdt
        .props1
        .bm.get
      val wideBgBlk = for {
        _       <- OptionUtil.maybeTrue( bm.wide )
        bg      <- stripJdt.props1.bgImg
        // 2018-01-23: Для wide-фона нужен отдельный блок, т.к. фон позиционируется отдельно от wide-block-контента.
        // TODO Нужна поддержка wide-фона без картинки.
        bgEdge  <- brd.edges.get( bg.imgEdge.edgeUid )
        imgWh   <- bgEdge.origWh
      } yield {
        imgWh
      }
      MGbBlock( bm, wideBgBlk )
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
                scAdData.focused.fold [IGbBlockPayload] {
                  // Несфокусированная карточка. Вернуть bm единственного стрипа.
                  val brd = scAdData.main
                  __blockRenderData2GbPayload( brd.template, brd )
                } { foc =>
                  // Открытая карточка. Вернуть MGbSubItems со списком фокус-блоков:
                  MGbSubItems {
                    val focBlk = foc.blkData
                    // Пройтись по блокам из focused-контейнера...
                    focBlk
                      .template
                      .subForest
                      .iterator
                      .map { __blockRenderData2GbPayload( _, focBlk ) }
                      .toList
                  }
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

          // Групповое выделение цветом обводки блоков, когда карточка раскрыта:
          groupOutlineColorOpt = ad.focused
            .toOption
            .flatMap { _ =>
              ad.main.template.rootLabel.props1.bgColor
            }
          // Сборка контейнера настроек рендера всех плиток группы:
          jdRenderArgs = groupOutlineColorOpt.fold(MJdRenderArgs.empty) { _ =>
            MJdRenderArgs(
              groupOutLined = groupOutlineColorOpt
            )
          }

          // Пройтись по шаблонам карточки
          (tpl2, j) <- ad.flatGridTemplates.iterator.zipWithIndex

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
                template = tpl2,
                edges    = edges,
                jdCss    = mgrid.jdCss,
                conf     = mgrid.jdConf,
                renderArgs = jdRenderArgs
              )
            } ( jdR.apply )
          )
        }
        val arr = iter.toVdomArray
        arr
      }
    }

  }


  val component = ScalaComponent.builder[Props]("GridCore")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(gridProxy: Props) = component(gridProxy)

}
