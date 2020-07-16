package io.suggest.sc.v.menu

import com.materialui.{MuiDivider, MuiListItem, MuiListItemClasses, MuiListItemProps, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps}
import diode.react.{ModelProxy, ReactConnectProxy}
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
import io.suggest.sc.m.inx.save.MIndexesRecentOuter
import io.suggest.sc.v.search.SearchCss
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.JsUnivEqUtil._
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
                    searchCssC              : ReactConnectProxy[SearchCss],
                    inxRecentsC             : ReactConnectProxy[MIndexesRecentOuter],
                  )


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      React.Fragment(
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
          s.inxRecentsC { intInfoProxy =>
            val inxRecents = intInfoProxy.value
            ReactCommonUtil.maybeEl( inxRecents.saved.exists(_.recents.nonEmpty) ) {
              val mroot = p.value
              val currInxState = mroot.index.state
              val distanceToOrNull = currInxState.inxGeoPoint.orNull

              val rowsData = for {
                inxInfo <- inxRecents.recentIndexes
              } yield {
                MNodesFoundRowProps(
                  node = MGeoNodePropsShapes(
                    props = inxInfo.indexResp,
                  ),
                  searchCss = inxRecents.searchCss,
                  // Чтобы не перерендеривать на каждый чих, пока без расстояний.
                  withDistanceToNull = distanceToOrNull,
                  selected = {
                    inxInfo.indexResp.nodeId.exists {
                      currInxState.rcvrId.contains[String]
                    } || inxInfo.indexResp.geoPoint.exists { mgp0 =>
                      currInxState.inxGeoPoint.exists(_ ~= mgp0)
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
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        searchCssC = propsProxy.connect { mroot =>
          mroot.internals.info.indexesRecents.searchCss
        },

        inxRecentsC = propsProxy.connect {
          _.internals.info.indexesRecents
        } (
          FastEqUtil { (a, b) =>
            a.saved ===* b.saved
          }
        ),

      )
    }
    .renderBackend[Backend]
    .build

}
