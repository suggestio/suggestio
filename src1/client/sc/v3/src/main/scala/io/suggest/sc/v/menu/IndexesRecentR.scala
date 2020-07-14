package io.suggest.sc.v.menu

import com.materialui.{MuiDivider, MuiListItem, MuiListItemClasses, MuiListItemProps, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.CssR
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.{IndexRecentNodeClick, MScReactCtx, MScRoot}
import io.suggest.sc.m.search.MNodesFoundRowProps
import io.suggest.sc.v.search.found.{NfListR, NfRowR}
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.m.in.MInternalInfo
import io.suggest.sc.v.search.SearchCss
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.2020 12:25
  * Description: Рендер недавно-посещённых узлов.
  */
class IndexesRecentR(
                      nfListR                  : NfListR,
                      nfRowR                   : NfRowR,
                      crCtxP                   : React.Context[MCommonReactCtx],
                      scReactCtxP              : React.Context[MScReactCtx],
                    ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    isVisibleSomeC          : ReactConnectProxy[Some[Boolean]],
                    searchCssC              : ReactConnectProxy[SearchCss],
                    intInfoC                : ReactConnectProxy[MInternalInfo],
                  )


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      val content = React.Fragment(
        MuiListItem()(
          MuiDivider()
        ),

        MuiListItem {
          val css = new MuiListItemClasses {
            override val root = ScCssStatic.Menu.Rows.rowLink.htmlClass
          }
          new MuiListItemProps {
            override val disableGutters = true
            override val classes = css
          }
        } (
          {
            val chs = <.span(
              ScCssStatic.Menu.Rows.rowContent,
              crCtxP.message( MsgCodes.`Recents` ),
              HtmlConstants.COLON,
            )
            scReactCtxP.consume { scReactCtx =>
              MuiListItemText {
                val textCss = new MuiListItemTextClasses {
                  override val root = scReactCtx.scCss.fgColor.htmlClass
                }
                new MuiListItemTextProps {
                  override val classes = textCss
                }
              } ( chs )
            }
          },
        ),

        s.searchCssC( CssR.compProxied.apply ),

        nfListR.component(
          nfListR.PropsVal()
        )(
          // Отрендерить список рядов:
          s.intInfoC { intInfoProxy =>
            val intInfo = intInfoProxy.value
            ReactCommonUtil.maybeEl( intInfo.inxRecentsClean.nonEmpty ) {
              val mroot = p.value
              val rowsData = for {
                // toSeq - чтобы скастовать List к Seq, которая требуется на выходе.
                inxInfo <- intInfo.inxRecentsClean
              } yield {
                MNodesFoundRowProps(
                  node = MGeoNodePropsShapes(
                    props = inxInfo.indexResp,
                  ),
                  searchCss = intInfo.indexesRecents.searchCss,
                  // Чтобы не перерендеривать на каждый чих, пока без расстояний.
                  //withDistanceToNull = distanceToOrNull,
                  selected = {
                    inxInfo.indexResp.nodeId.exists {
                      mroot.index.state.rcvrId.contains[String]
                    } || inxInfo.indexResp.geoPoint.exists { mgp0 =>
                      mroot.index.state.inxGeoPoint.exists(_ ~= mgp0)
                    }
                  },
                )
              }
              val rowsProxy = p.resetZoom[Seq[MNodesFoundRowProps]]( rowsData )
              nfRowR( rowsProxy )( IndexRecentNodeClick )
            }
          },
        ),
      )

      // Обернуть список в результат:
      s.isVisibleSomeC { isVisibleSomeProxy =>
        val isVisible = isVisibleSomeProxy.value.value
        ReactCommonUtil.maybeEl( isVisible )( content )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        isVisibleSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.internals.info.inxRecentsClean.nonEmpty )
        },

        searchCssC = propsProxy.connect { mroot =>
          mroot.internals.info.indexesRecents.searchCss
        },

        intInfoC = propsProxy.connect( _.internals.info )(
          FastEqUtil[MInternalInfo] { (a, b) =>
            // Сравниваем lazyVal'ы, т.к. референсно они повторяют поля case class'а
            (a.inxRecentsClean ===* b.inxRecentsClean)
          }
        ),

      )
    }
    .renderBackend[Backend]
    .build

}
