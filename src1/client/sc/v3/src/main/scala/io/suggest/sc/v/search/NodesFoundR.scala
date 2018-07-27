package io.suggest.sc.v.search

import diode.react.ReactPot._
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.{MNodesFoundS, NodeRowClick, NodesScroll}
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromHtml, ScalaComponent}
import japgolly.univeq._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.07.18 18:28
  * Description: React-компонент, отвечающий за рендер списка найденных узлов: тегов и гео-размещенных узлов.
  */
class NodesFoundR(
                   getScCssF   : GetScCssF
                 ) {

  type Props_t = MNodesFoundS
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на клик по одному элементу (ряду, узлу). */
    private def _onNodeRowClick(nodeId: String): Callback = {
      dispatchOnProxyScopeCB($, NodeRowClick(nodeId))
    }

    /** Скроллинг в списке найденных узлов. */
    private def _onNodesListScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      dispatchOnProxyScopeCB($, NodesScroll(scrollTop, scrollHeight))
    }


    def render( nodeSearchProxy: ModelProxy[MNodesFoundS]): VdomElement = {
      val scCss = getScCssF()
      val NodesCSS = scCss.Search.Tabs.NodesFound

      val mNodesSearch = nodeSearchProxy.value
      val _tagRow = NodesCSS.nodeRow: TagMod
      val _odd = NodesCSS.oddRow: TagMod
      val _even = NodesCSS.evenRow: TagMod

      <.div(
        NodesCSS.listDiv,

        // Подписка на события скроллинга:
        ReactCommonUtil.maybe(mNodesSearch.hasMore && !mNodesSearch.req.isPending) {
          ^.onScroll ==> _onNodesListScroll
        },

        // Рендер нормального списка найденных узлов.
        mNodesSearch.req.render { tagsRi =>
          if (tagsRi.resp.isEmpty) {
            <.div(
              _tagRow,
              // Надо, чтобы юзер понимал, что запрос поиска отработан.
              tagsRi.textQuery.fold {
                Messages( MsgCodes.`No.tags.here` )
              } { query =>
                Messages( MsgCodes.`No.tags.found.for.1.query`, query )
              }
            )
          } else {
            tagsRi
              .resp
              .iterator
              .zipWithIndex
              .toVdomArray { case (mtag, i) =>
                val p = mtag.props
                // Рендер одного ряда.
                <.div(
                  _tagRow,

                  // Используем nodeId как ключ. Контроллер должен выверять отсутствие дубликатов в списке тегов.
                  ^.key := p.nodeId,

                  // Визуально разделять разные ряды.
                  if (i % 2 ==* 0) _odd else _even,

                  // Подсвечивать текущие выделенные теги.
                  ReactCommonUtil.maybe(mNodesSearch.selectedId contains p.nodeId) {
                    NodesCSS.selected
                  },

                  ^.onClick --> _onNodeRowClick(p.nodeId),

                  p.hint.whenDefined,

                  // Иконка узла.
                  p.icon.whenDefined { ico =>
                    <.img(
                      NodesCSS.icon,
                      ^.src := ico.url
                    )
                  }

                )
              }
          }

        },

        // Рендер крутилки ожидания.
        mNodesSearch.req.renderPending { _ =>
          <.div(
            _tagRow,
            LkPreLoaderR.AnimSmall
          )
        },

        // Рендер ошибки.
        mNodesSearch.req.renderFailed { ex =>
          VdomArray(
            <.div(
              _tagRow,
              ^.key := "e",
              ^.`class` := Css.Colors.RED,
              ex.getClass.getSimpleName
            ),
            <.div(
              ^.key := "m",
              _tagRow,
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
