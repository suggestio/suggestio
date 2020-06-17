package io.suggest.sc.v.search.found

import com.github.souporserious.react.measure.{ContentRect, Measure, MeasureChildrenArgs, MeasureProps}
import com.materialui.{MuiGrid, MuiGridClasses, MuiGridProps, MuiList, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps}
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.css.Css
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.m.search.{MNodesFoundS, MSearchRespInfo, NodesFoundListWh}
import io.suggest.sc.v.styl.ScCssStatic
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.07.18 18:28
  * Description: wrap-компонент, отвечающий за рендер списка найденных узлов: тегов и гео-размещенных узлов.
  */
final class NfListR(
                     crCtxProv              : React.Context[MCommonReactCtx],
                     scReactCtxProv         : React.Context[MScReactCtx],
                   ) {

  type Props_t = MNodesFoundS
  type Props = ModelProxy[Props_t]


  case class State(
                    reqPotC               : ReactConnectProxy[Pot[MSearchRespInfo[MGeoNodesResp]]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private val __onResizeJsCbF = ReactCommonUtil.cbFun1ToJsCb { contentRect: ContentRect =>
      $.props >>= { propsProxy: Props =>
        // Проверять $.props.rHeightPx.isPending?
        if (propsProxy.value.rHeightPx.isPending) {
          val b = contentRect.bounds.get
          val bounds2d = MSize2di(
            width  = b.width.toInt,
            height = b.height.toInt,
          )
          ReactDiodeUtil.dispatchOnProxyScopeCB( $, NodesFoundListWh(bounds2d) )
        } else {
          Callback.empty
        }
      }
    }


    def render(s: State, children: PropsChildren): VdomElement = {
      val NodesCSS = ScCssStatic.Search.NodesFound

      val content = MuiGrid {
        val listClasses = new MuiGridClasses {
          override val root = (NodesCSS.listDiv :: NodesCSS.nodesList :: Nil).toHtmlClass
        }
        new MuiGridProps {
          override val container = true
          override val classes = listClasses
        }
      } (

        // Рендер нормального списка найденных узлов.
        s.reqPotC { reqPotProxy =>
          React.Fragment(
            reqPotProxy.value.render { nodesRi =>
              if (nodesRi.resp.nodes.isEmpty) {
                // Надо, чтобы юзер понимал, что запрос поиска отработан.
                MuiGrid(
                  new MuiGridProps {
                    override val item = true
                  }
                )(
                  MuiList()(
                    scReactCtxProv.consume { scReactCtx =>
                      MuiListItemText {
                        val css = new MuiListItemTextClasses {
                          override val root = Css.flat(
                            scReactCtx.scCss.fgColor.htmlClass,
                            ScCssStatic.Search.NodesFound.nothingFound.htmlClass,
                          )
                        }
                        new MuiListItemTextProps {
                          override val classes = css
                        }
                      } (
                        {
                          val (msgCode, msgArgs) = nodesRi.textQuery.fold {
                            MsgCodes.`No.tags.here` -> List.empty[js.Any]
                          } { query =>
                            MsgCodes.`No.tags.found.for.1.query` -> ((query: js.Any) :: Nil)
                          }
                          crCtxProv.message( msgCode, msgArgs: _* )
                        }
                      )
                    }
                  )
                )
              } else {
                children
              }
            }
          )

        },

      )

      // Измерить список.
      Measure {
        def __mkChildrenF(args: MeasureChildrenArgs): raw.PropsChildren = {
          <.div(
            ^.genericRef := args.measureRef,
            content
          )
            .rawElement
        }
        new MeasureProps {
          override val bounds = true
          override val children = __mkChildrenF
          override val onResize = __onResizeJsCbF
        }
      }

    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        reqPotC = propsProxy.connect( _.req ),
      )
    }
    .renderBackendWithChildren[Backend]
    .build

}
