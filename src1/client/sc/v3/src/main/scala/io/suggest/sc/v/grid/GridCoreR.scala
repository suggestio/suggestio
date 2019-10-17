package io.suggest.sc.v.grid

import com.github.dantrain.react.stonecutter.{CSSGrid, GridComponents}
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.grid.GridBuilderUtilJs
import io.suggest.jd.render.m.{MJdArgs, MJdDataJs, MJdRenderArgs}
import io.suggest.jd.render.v.JdR
import io.suggest.react.ReactDiodeUtil
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.sc.m.grid._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactMouseEvent, ScalaComponent}
import japgolly.univeq._

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
                 jdR                        : JdR,
               ) {

  import MJdArgs.MJdArgsFastEq


  type Props_t = MGridCoreS
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Клик по карточке в плитке. */
    private def onBlockClick(nodeId: String)(e: ReactMouseEvent): Callback = {
      if( e.button ==* 0 ) {
        ReactDiodeUtil.dispatchOnProxyScopeCB($, GridBlockClick(nodeId))
      } else {
        Callback.empty
      }
    }


    def render(mgridProxy: Props): VdomElement = {
      val mgrid = mgridProxy.value

      CSSGrid {
        GridBuilderUtilJs.mkCssGridArgs(
          gbRes     = mgrid.gridBuild,
          conf      = mgrid.jdConf,
          tagName   = GridComponents.DIV
        )
      } {
        // TODO routerCtl.urlFor() внутри <a.href>
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
              ad.main.doc.template.rootLabel.props1.bgColor
            }

          // Сборка контейнера настроек рендера всех плиток группы:
          jdRenderArgs = groupOutlineColorOpt.fold(MJdRenderArgs.empty) { _ =>
            MJdRenderArgs(
              groupOutLined = groupOutlineColorOpt
            )
          }

          // Пройтись по шаблонам карточки
          (jdDoc2, j) <- ad.flatGridTemplates.zipWithIndex

        } yield {
          // Для скроллинга требуется повесить scroll.Element вокруг первого блока.
          // На телевизорах и прочих около-умных устройствах без нормальных устройств ввода,
          // для кликов подсвечиваются только ссылки.
          // Поэтому тут используется <A>-тег, хотя div был бы уместнее.
          <.a(
            ^.key := (rootId + `.` + j),

            // Реакция на клики, когда nodeId задан.
            ad.nodeId.whenDefined { nodeId =>
              ^.onClick ==> onBlockClick(nodeId)
            },

            {
              val jdArgsProxy2 = mgridProxy.resetZoom(
                MJdArgs(
                  data = MJdDataJs(
                    doc      = jdDoc2,
                    edges    = edges,
                  ),
                  jdRuntime   = mgrid.jdRuntime,
                  conf        = mgrid.jdConf,
                  renderArgs  = jdRenderArgs,
                )
              )
              // Нельзя одновременно использовать разные инстансы mgrid, поэтому для простоты и удобства используем только внешний.
              jdR.apply(jdArgsProxy2)
            }
          )
        }
        iter.toVdomArray
      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  private def _apply(gridProxy: Props) = component(gridProxy)
  val apply: ReactConnectProps[Props_t] = _apply

}
