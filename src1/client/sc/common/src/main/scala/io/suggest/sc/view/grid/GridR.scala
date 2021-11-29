package io.suggest.sc.view.grid

import com.github.dantrain.react.stonecutter.{CSSGrid, GridComponents}
import com.materialui.{MuiCircularProgress, MuiCircularProgressClasses, MuiCircularProgressProps, MuiColorTypes}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.css.CssR
import io.suggest.grid.{GridBuilderUtilJs, GridConst, IGridRenderer, ScGridScrollUtil}
import io.suggest.jd.render.m.{MJdArgs, MJdDataJs, MJdRenderArgs}
import io.suggest.jd.render.v.{JdCss, JdCssStatic, JdR, JdRrr}
import io.suggest.n2.edge.MEdgeFlags
import io.suggest.react.ReactDiodeUtil.Implicits.ModelProxyExt
import io.suggest.react.r.CatchR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.ScConstants.ScJsState
import io.suggest.sc.model.MScRoot
import io.suggest.sc.model.grid.{GridAdKey_t, GridBlockClick, GridScroll, MGridCoreS}
import io.suggest.sc.util.ScGridItemEventListener
import io.suggest.sc.view.styl.{ScCss, ScCssStatic}
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.tv.SmartTvUtil
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.spa.SioPages
import io.suggest.xplay.json.PlayJsonSjsUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html
import scalacss.ScalaCssReact._
import japgolly.univeq._
import org.scalajs.dom
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 14:38
  * Description: React-компонент плитки карточек.
  */
class GridR(
             jdCssStatic                : JdCssStatic,
             gridRenderer               : IGridRenderer,
             scCssP                     : React.Context[ScCss],
             jdRrr                      : JdRrr,
             jdR                        : JdR,
           ) {


  type Props_t = MScRoot
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
      // Надо тут оценить необходимость подгрузки карточек, проводить сравнение, и прямо тут порешать - нужно ли диспатчить GridScroll или нет.
      // Эта функция вызывается при скролле десятки раз, поэтому нужно минимизировать нагрузку от неё. А желательно вообще загнать в passive event listener.
      val scrollTop = e.target.scrollTop
      $.props >>= { propsProxy: Props =>
        Callback.when {
          val props = propsProxy.value.grid
          props.hasMoreAds &&
          !props.core.ads.adsTreePot.isPending && {
            // Оценить уровень скролла. Возможно, уровень не требует подгрузки ещё карточек
            val contentHeight = props.core.gridBuild.gridWh.height + GridConst.CONTAINER_OFFSET_TOP
            val screenHeight = dom.window.innerHeight
            val scrollPxToGo = contentHeight - screenHeight - scrollTop
            scrollPxToGo < GridConst.LOAD_MORE_SCROLL_DELTA_PX
          }
        } {
          propsProxy.dispatchCB( GridScroll )
        }
      }
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
      // Непосредственный рендер плитки - коннекшен в отдельный компонент, снаружи от рендера connect-зависимого контейнера плитки.
      val gridCore = s.gridCoreC { mgridCoreProxy =>
        val mgridCore = mgridCoreProxy.value
        val mroot = p.value

        gridRenderer( mgridCore ) {
          for {
            adPtrsTree <- mgridCore.ads.adsTreePot.iterator

            isLinkContainer = gridRenderer.preferLinkContainer || SmartTvUtil.isSmartTvUserAgentCached
            // Prepare possible js-routes generator:
            linkJsRoutesOpt = mroot.internals.jsRouter.jsRoutesOpt
              .filter( _ => isLinkContainer )

            // На телевизорах и прочих около-умных устройствах без нормальных устройств ввода,
            // для кликов подсвечиваются только ссылки.
            // Поэтому для SmartTV используется <A>-тег, хотя это вызовет ошибку ссылка-внутри-ссылки и ругань react-dev.
            gridElTag: TagOf[html.Element] = {
              if (isLinkContainer) <.a
              else <.div
            }

            scAdDataLoc <- adPtrsTree.loc.onlyLowest.iterator
            scAdData = scAdDataLoc.getLabel
            // TODO Pot().iterator: Надо отрабатывать рендером pending и прочие состояния конкретной карточки.
            adData <- scAdData.data.iterator

            // TODO Нужно тут как-то перехватывать возможные ошибки рендера текущей карточки, чтобы нарушение в рендере одной карточки не приводило к падению всего.

            // Групповое выделение цветом обводки блоков, когда карточка раскрыта:
            groupOutlinedColorOpt = for {
              adDataForJdRenderArgs <- Option.when(
                adData.info.flags.exists(_.flag ==* MEdgeFlags.AlwaysOutlined) ||
                adData.isOpened
              )(adData)
              bgColor <- adDataForJdRenderArgs.doc.template.getMainBgColor
            } yield {
              bgColor
            }

            // Получить стабильный путь до карточки в дереве:
            scAdDataPath = scAdDataLoc.gridKeyPath

            // Пройтись по шаблонам карточки
            gridItem <- scAdData.gridItems.iterator

            jdRenderArgs = MJdRenderArgs(
              groupOutLined = groupOutlinedColorOpt,
              eventListener = Some {
                new ScGridItemEventListener(
                  propsProxy  = p,
                  gridItem    = gridItem,
                  gridPath    = scAdDataPath,
                  jdDataJs    = adData,
                )
              },
            )

          } yield {
            // Для скроллинга требуется повесить scroll.Element вокруг первого блока.
            val resultTag = gridElTag(
              ^.key := gridItem.gridKey,

              // Render a.href, if link container is used. Do not using RouterCtl[] here, because this is mostly
              // server-side feature, optional and rare for browser-side flow (because it produces nested
              // anchor's: outer block href + possible inside-content links).
              (for {
                jsRoutes <- linkJsRoutesOpt
                if isLinkContainer
                adIdOpt = adData.doc.tagId.nodeId
                if adIdOpt.nonEmpty
              } yield {
                val route = jsRoutes.controllers.sc.ScSite.geoSite(
                  PlayJsonSjsUtil.toNativeJsonObj(
                    Json.toJsObject(
                      SioPages.Sc3(
                        nodeId      = mroot.index.state.rcvrId,
                        focusedAdId = adIdOpt,
                      )
                    )
                  ),
                )

                val url = route.url
                // TODO Don't know, if absolute URLs are needed here, because abs.url may lead crawlers from base 3p-domain into suggest.io.
                //      val url = if (HttpClient.PREFER_ABS_URLS ) route.absoluteURL( HttpClient.PREFER_SECURE_URLS ) else route.url

                ^.href := ScJsState.fixJsRouterUrl( url )
              })
                .whenDefined,

              ^.onClick ==> onGridItemClick( scAdDataPath, gridItem.gridKey ),

              // Выставить класс для ремонта z-index контейнера блока.
              jdRrr.fixZIndexIfBlock( gridItem.jdDoc.template.rootLabel ),

              CatchR.component {
                p.resetZoom( gridItem.jdDoc.tagId.toString )
              } (
                jdR {
                  // Нельзя одновременно использовать разные инстансы mgrid, поэтому для простоты и удобства используем только внешний.
                  mgridCoreProxy.resetZoom(
                    MJdArgs(
                      data        = (MJdDataJs.doc replace gridItem.jdDoc)(adData),
                      jdRuntime   = mgridCore.jdRuntime,
                      conf        = mgridCore.jdConf,
                      renderArgs  = jdRenderArgs,
                    )
                  )
                },
              ),

            )

            gridItem.gridKey -> resultTag
          }
        }
      }

      // Рендер jd-css:
      val jdCssStatic1 = CssR.compProxied( p.resetZoom( jdCssStatic ) )
      val jdCss1 = s.jdCssC( CssR.compProxied.apply )

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
            ^.id := ScGridScrollUtil.SCROLL_CONTAINER_ID,
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
    .initialStateFromProps { mrootProxy =>
      val gridCoreProxy = mrootProxy.zoom(_.grid.core)

      State(
        jdCssC = gridCoreProxy.connect(_.jdRuntime.jdCss)( JdCss.JdCssFastEq ),

        gridSzC = gridCoreProxy.connect( _.gridBuild.gridWh )( FastEq.ValueEq ),

        gridCoreC = gridCoreProxy.connect( identity ),

        loaderPotC = gridCoreProxy.connect { props =>
          OptionUtil.SomeBool( props.ads.adsTreePot.isPending )
        }( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

}
