package io.suggest.sc.v.grid

import com.github.dantrain.react.stonecutter.{CSSGrid, GridComponents}
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.grid.GridBuilderUtilJs
import io.suggest.jd.render.m.{MJdArgs, MJdDataJs, MJdRenderArgs}
import io.suggest.jd.render.u.JdUtil
import io.suggest.jd.render.v.JdR
import io.suggest.model.n2.edge.MEdgeFlags
import io.suggest.react.ReactDiodeUtil
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.sc.m.grid._
import io.suggest.tv.SmartTvUtil
import japgolly.scalajs.react.vdom.{TagOf, VdomElement}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactMouseEvent, ScalaComponent}
import japgolly.univeq._
import org.scalajs.dom.html

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
        // На телевизорах и прочих около-умных устройствах без нормальных устройств ввода,
        // для кликов подсвечиваются только ссылки.
        // Поэтому для SmartTV используется <A>-тег, хотя это вызовет ошибку ссылка-внутри-ссылки и ругань react-dev.
        val gridElTag: TagOf[html.Element] =
          if (SmartTvUtil.isSmartTvUserAgentCached) <.a
          else <.div

        // TODO routerCtl.urlFor() внутри <a.href>
        (for {
          ad <- mgrid.ads
            .iterator
            .flatten

          adData = ad.focOrMain

          // Групповое выделение цветом обводки блоков, когда карточка раскрыта:
          jdRenderArgs = (for {
            adDataForJdRenderArgs <- {
              if (adData.info.flags.exists(_.flag ==* MEdgeFlags.AlwaysOutlined))
                Some(adData)
              else
                ad.focOrAlwaysOpened
            }
            bgColorOpt = adDataForJdRenderArgs.doc.template
              .getMainBlockOrFirst
              .rootLabel
              .props1
              .bgColor
            if bgColorOpt.nonEmpty
          } yield {
            MJdRenderArgs(
              groupOutLined = bgColorOpt
            )
          })
            .getOrElse( MJdRenderArgs.empty )

          // Пройтись по шаблонам карточки
          jdDoc2 <- JdUtil.flatGridTemplates( adData )

        } yield {
          // Для скроллинга требуется повесить scroll.Element вокруг первого блока.
          gridElTag(
            // TODO key: Какой ключ генерить, когда одна карточка повторяется в плитке? Сейчас этого нет, но это ведь возможно в будущем.
            ^.key := jdDoc2.jdId.toString,

            // Реакция на клики, когда nodeId задан.
            ad.nodeId.whenDefined { nodeId =>
              ^.onClick ==> onBlockClick(nodeId)
            },

            // Выставить класс для ремонта z-index контейнера блока.
            jdR.fixZIndexIfBlock( jdDoc2.template.rootLabel ),

            jdR.apply {
              // Нельзя одновременно использовать разные инстансы mgrid, поэтому для простоты и удобства используем только внешний.
              mgridProxy.resetZoom(
                MJdArgs(
                  data        = (MJdDataJs.doc set jdDoc2)(adData),
                  jdRuntime   = mgrid.jdRuntime,
                  conf        = mgrid.jdConf,
                  renderArgs  = jdRenderArgs,
                )
              )
            }

          )
        })
          .toVdomArray
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  private def _apply(gridProxy: Props) = component(gridProxy)
  val apply: ReactConnectProps[Props_t] = _apply

}
