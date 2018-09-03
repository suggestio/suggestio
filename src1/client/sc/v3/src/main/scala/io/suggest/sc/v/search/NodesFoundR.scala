package io.suggest.sc.v.search

import diode.FastEq
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.geo.MGeoLoc
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.{MSearchRespInfo, NodesScroll}
import io.suggest.sc.styl.GetScCssF
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromHtml, ScalaComponent}
import japgolly.univeq._
import scalacss.ScalaCssReact._
import io.suggest.maps.nodes.MGeoNodePropsShapes

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.07.18 18:28
  * Description: React-компонент, отвечающий за рендер списка найденных узлов: тегов и гео-размещенных узлов.
  */
class NodesFoundR(
                   nodeFoundR       : NodeFoundR,
                   getScCssF        : GetScCssF
                 ) {

  /** Контейнер данных для рендера компонента.
    *
    * @param found Данные найденных тегов/узлов.
    * @param withDistanceTo Рендерить расстояние до указанной локации. Инстанс mapInit.userLoc.
    */
  case class PropsVal(
                       req              : Pot[MSearchRespInfo[Seq[MGeoNodePropsShapes]]],
                       hasMore          : Boolean,
                       selectedIds      : Set[String],
                       withDistanceTo   : Option[MGeoLoc],
                       searchCss        : SearchCss
                     )
  implicit object NodesFoundRPropsValFastEq extends FastEq[PropsVal] {
    import io.suggest.ueq.JsUnivEqUtil._
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.req ===* b.req) &&
        (a.hasMore ==* b.hasMore) &&
        (a.selectedIds ===* b.selectedIds) &&
        (a.withDistanceTo ===* b.withDistanceTo) &&
        // Наверное, проверять css не нужно. Но мы всё же перерендериваем.
        (a.searchCss ===* b.searchCss)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    /** Скроллинг в списке найденных узлов. */
    private def _onNodesListScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      dispatchOnProxyScopeCB($, NodesScroll(scrollTop, scrollHeight) )
    }


    def render(propsProxy: Props): VdomElement = {
      val scCss = getScCssF()
      val NodesCSS = scCss.Search.Tabs.NodesFound

      val props = propsProxy.value
      val _tagRowCss = NodesCSS.nodeRow: TagMod

      <.div(
        NodesCSS.listDiv,
        props.searchCss.NodesFound.nodesList,

        // Подписка на события скроллинга:
        ReactCommonUtil.maybe(props.hasMore && !props.req.isPending) {
          ^.onScroll ==> _onNodesListScroll
        },

        // Рендер нормального списка найденных узлов.
        props.req.render { nodesRi =>
          if (nodesRi.resp.isEmpty) {
            <.div(
              _tagRowCss,
              // Надо, чтобы юзер понимал, что запрос поиска отработан.
              nodesRi.textQuery.fold {
                Messages( MsgCodes.`No.tags.here` )
              } { query =>
                Messages( MsgCodes.`No.tags.found.for.1.query`, query )
              }
            )
          } else {
            nodesRi
              .resp
              .iterator
              .zipWithIndex
              .toVdomArray { case (node, i) =>
                // Рендер одного ряда. На уровне компонента обитает shouldComponentUpdate() для
                propsProxy.wrap { props =>
                  nodeFoundR.PropsVal(
                    node            = node,
                    i               = i,
                    searchCss       = props.searchCss,
                    withDistanceTo  = props.withDistanceTo,
                    selected        = props.selectedIds contains node.props.nodeId
                  )
                }( nodeFoundR.component.withKey(node.props.nodeId)(_) )
              }
          }
        },

        // Рендер крутилки ожидания.
        props.req.renderPending { _ =>
          <.div(
            _tagRowCss,
            LkPreLoaderR.AnimSmall
          )
        },

        // Рендер ошибки.
        props.req.renderFailed { ex =>
          VdomArray(
            <.div(
              _tagRowCss,
              ^.key := "e",
              ^.`class` := Css.Colors.RED,
              ex.getClass.getSimpleName
            ),
            <.div(
              ^.key := "m",
              _tagRowCss,
              ex.getMessage
            )
          )
        }

      ) // nodesList
    }

  }

  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(nodesFound: Props) = component( nodesFound )

}
