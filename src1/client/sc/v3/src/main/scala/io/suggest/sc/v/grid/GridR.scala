package io.suggest.sc.v.grid

import com.github.dantrain.react.stonecutter.{CSSGrid, GridComponents}
import com.materialui.{MuiCircularProgress, MuiCircularProgressClasses, MuiCircularProgressProps, MuiColorTypes}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.css.CssR
import io.suggest.grid.{GridBuilderUtilJs, GridConst, GridScrollUtil}
import io.suggest.jd.render.m.{MJdArgs, MJdDataJs, MJdRenderArgs}
import io.suggest.jd.render.v.{JdCss, JdCssStatic, JdR, JdRrr}
import io.suggest.n2.edge.MEdgeFlags
import io.suggest.react.ReactDiodeUtil.Implicits.ModelProxyExt
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.grid.{GridAdKey_t, GridBlockClick, GridScroll, MGridCoreS, MGridS}
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import io.suggest.tv.SmartTvUtil
import io.suggest.scalaz.ZTreeUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.{TagOf, VdomElement}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html
import scalacss.ScalaCssReact._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 14:38
  * Description: React-компонент плитки карточек.
  */
class GridR(
             jdCssStatic                : JdCssStatic,
             scCssP                     : React.Context[ScCss],
             jdRrr                      : JdRrr,
             jdR                        : JdR,
           ) {


  type Props_t = MGridS
  type Props = ModelProxy[Props_t]


  /** Модель состояния компонента. */
  protected[this] case class State(
                                    jdCssC              : ReactConnectProxy[JdCss],
                                    gridSzC             : ReactConnectProxy[MSize2di],
                                    gridCoreC           : ReactConnectProxy[MGridCoreS],
                                    loaderPotC          : ReactConnectProxy[Some[Boolean]],
                                  )

  class Backend($: BackendScope[Props, State]) {

    /** Скроллинг плитки. */
    private def onGridScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GridScroll(scrollTop))
    }

    /** Клик по карточке в плитке.
      *
      * @param gridKeyPath Путь до карточки в дереве adsTree.
      * @param gridItemKey Ключ gridItem в карточке.
      * @param e Исходное событие.
      * @return Callback.
      */
    private def onGridItemClick(gridKeyPath: List[GridAdKey_t], gridItemKey: GridAdKey_t)
                               (e: ReactMouseEvent): Callback = {
      Callback.when( e.button ==* 0 ) {
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, GridBlockClick(
          gridPath = Some( gridKeyPath ),
          gridKey  = Some( gridItemKey ),
        ))
      }
    }


    def render(p: Props, s: State): VdomElement = {
      // Рендер jd-css:
      val jdCssStatic1 = p.wrap(_ => jdCssStatic)( CssR.compProxied.apply )
      val jdCss1 = s.jdCssC { CssR.compProxied.apply }

      // Непосредственный рендер плитки - коннекшен в отдельный компонент, снаружи от рендера connect-зависимого контейнера плитки.
      val gridCore = s.gridCoreC { mgridProxy =>
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

          (for {
            adPtrsTree <- mgrid.ads.adsTreePot.iterator
            scAdDataLoc <- adPtrsTree.loc.onlyLowest.iterator
            scAdData = scAdDataLoc.getLabel
            // TODO Pot().iterator: Надо отрабатывать рендером pending и прочие состояния конкретной карточки.
            adData <- scAdData.data.iterator

            // Групповое выделение цветом обводки блоков, когда карточка раскрыта:
            jdRenderArgs = (for {
              adDataForJdRenderArgs <- Option.when(
                adData.info.flags.exists(_.flag ==* MEdgeFlags.AlwaysOutlined) ||
                adData.isOpened
              )(adData)
              bgColorOpt = adDataForJdRenderArgs.doc.template.getMainBgColor
              if bgColorOpt.nonEmpty
            } yield {
              MJdRenderArgs(
                groupOutLined = bgColorOpt,
              )
            })
              .getOrElse( MJdRenderArgs.empty )

            // Получить стабильный путь до карточки в дереве:
            scAdDataPath = scAdDataLoc.gridKeyPath

            // Пройтись по шаблонам карточки
            gridItem <- scAdData.gridItems.iterator

          } yield {
            // Для скроллинга требуется повесить scroll.Element вокруг первого блока.
            gridElTag(
              ^.key := gridItem.gridKey,

              // TODO routerCtl.urlFor() внутри <a.href> ?

              // Реакция на клики, когда nodeId задан.
              ^.onClick ==> onGridItemClick( scAdDataPath, gridItem.gridKey ),

              // Выставить класс для ремонта z-index контейнера блока.
              jdRrr.fixZIndexIfBlock( gridItem.jdDoc.template.rootLabel ),

              jdR {
                // Нельзя одновременно использовать разные инстансы mgrid, поэтому для простоты и удобства используем только внешний.
                mgridProxy.resetZoom(
                  MJdArgs(
                    data        = (MJdDataJs.doc set gridItem.jdDoc)(adData),
                    jdRuntime   = mgrid.jdRuntime,
                    conf        = mgrid.jdConf,
                    renderArgs  = jdRenderArgs,
                  )
                )
              },

            )
          })
            .toVdomArray
        }
      }

      val smFlex = ScCssStatic.smFlex: TagMod

      val gridContent = {
        val chs0 = TagMod(
          ScCssStatic.Grid.container,

          jdCssStatic1,
          jdCss1,
          gridCore,
        )
        s.gridSzC { gridSzProxy =>
          scCssP.consume { scCss =>
            val gridSz = gridSzProxy.value
            <.div(
              scCss.Grid.container,
              ^.width  := gridSz.width.px,
              ^.height := (gridSz.height + GridConst.CONTAINER_OFFSET_BOTTOM + GridConst.CONTAINER_OFFSET_TOP).px,
              chs0,
            )
          }
        }
      }

      // Крутилка подгрузки карточек.
      val loader = s.loaderPotC { adsPotPendingSomeProxy =>
        ReactCommonUtil.maybeEl( adsPotPendingSomeProxy.value.value ) {
          scCssP.consume { scCss =>
          MuiCircularProgress {
            val cssClasses = new MuiCircularProgressClasses {
              override val root = scCss.Grid.loader.htmlClass
            }
            new MuiCircularProgressProps {
              override val color   = MuiColorTypes.secondary
              override val classes = cssClasses
            }
          }
          }
          
        }
      }

      scCssP.consume { scCss =>
        val GridCss = scCss.Grid

        <.div(
          smFlex, GridCss.outer, scCss.bgColor,

          <.div(
            smFlex, GridCss.wrapper,
            ^.id := GridScrollUtil.SCROLL_CONTAINER_ID,
            ^.onScroll ==> onGridScroll,

            <.div(
              GridCss.content,

              // Всея плитка:
              gridContent,

              loader,

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

        gridCoreC = propsProxy.connect(_.core),

        loaderPotC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.core.ads.adsTreePot.isPending )
        }( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

}
