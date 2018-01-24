package io.suggest.sc.v.grid

import com.github.dantrain.react.stonecutter.{CSSGrid, GridComponents}
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.grid.build._
import io.suggest.jd.render.m.{MJdArgs, MJdRenderArgs}
import io.suggest.jd.render.v.{JdGridUtil, JdR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.grid.{GridBlockClick, HandleGridBuildRes, MGridS}
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
                  val stripJdt = scAdData.main.template
                    .rootLabel
                  val bm = stripJdt
                    .props1
                    .bm.get
                  MGbBlock( bm )
                } { foc =>
                  // Открытая карточка. Вернуть Right() со списком фокус-блоков:
                  MGbSubItems {
                    // Пройтись по блокам из focused-контейнера...
                    foc.template
                      .subForest
                      .iterator
                      .map { subStripJdt =>
                        val stripJdt = subStripJdt.rootLabel
                        val bm = stripJdt
                          .props1
                          .bm.get
                        val wideBgBlk = for {
                          _       <- OptionUtil.maybeTrue( bm.wide )
                          bg      <- stripJdt.props1.bgImg
                          // 2018-01-23: Для wide-фона нужен отдельный блок, т.к. фон позиционируется отдельно от wide-block-контента.
                          // TODO Нужна поддержка wide-фона без картинки.
                          bgEdge  <- foc.edges.get( bg.imgEdge.edgeUid )
                          imgWh   <- bgEdge.origWh
                        } yield {
                          println( "wideBgSz 4 = " + imgWh )
                          imgWh
                        }
                        MGbBlock(bm, wideBgSz = wideBgBlk )
                      }
                      .toList
                  }
                }
              }
              .toList
            ,
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
          (tpl0, j) <- ad.flatGridTemplates.iterator.zipWithIndex
          // 2018-01-23: Для wide-фона нужен отдельный блок, т.к. фон позиционируется отдельно от wide-block-контента.
          // Распиливать шаблон wide-стрипа на два похожих: фон + контент.
          // TODO Нужна поддержка чистого wide-фона без фоновой картинки.
          (tpl2, k) <- {
            val jdt0 = tpl0.rootLabel
            jdt0.props1.bm
              .iterator
              .flatMap { bm =>
                if (bm.wide && jdt0.props1.bgImg.nonEmpty) {
                  // Широкий блок с фоном: рендерить фон, отдельно рендерить блок фона, отдельно - блок с контентом.
                  val contentTpl = Tree.Node(
                    root = jdt0.withProps1(
                      jdt0.props1
                        .withBgImg( None )
                        .withBgColor( None )
                    ),
                    forest = tpl0.subForest
                  )
                  val bgTpl = Tree.Leaf(
                    root = jdt0
                  )
                  println( "wide bg render: " + jdt0 )
                  bgTpl :: contentTpl :: Nil
                } else {
                  // Это не-wide-блок.
                  tpl0 :: Nil
                }
              }
              .zipWithIndex
          }

        } yield {
          // На телевизорах и прочих около-умных устройствах без нормальных устройств ввода,
          // для кликов подсвечиваются только ссылки.
          // Поэтому тут используется <A>-тег, хотя div был бы уместнее.
          <.a(
            ^.key := (rootId + `.` + j + (if (k > 0) `.` + k else "")),

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
                renderArgs = if (tpl0 eq tpl2)
                  MJdRenderArgs.empty
                else MJdRenderArgs(
                  blockStyleFrom = Some(tpl0.rootLabel)
                )
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
