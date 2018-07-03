package io.suggest.sc.v.search

import diode.react.ReactPot._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.{MTagsSearchS, TagClick, TagsScroll}
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromHtml, ScalaComponent}
import japgolly.univeq._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.17 16:53
  * Description: React-компонент для поиска и выборки тегов.
  * Использовать через wrap().
  */
class TagsSearchR(
                   getScCssF   : GetScCssF
                 ) {

  import MTagsSearchS.MTagsSearchFastEq

  type Props = ModelProxy[MTagsSearchS]


  protected case class State(
                              tagsC        : ReactConnectProxy[MTagsSearchS]
                            )

  class Backend($: BackendScope[Props, State]) {

    /** Реакция на клик по одному тегу. */
    private def _onTagClick(nodeId: String): Callback = {
      dispatchOnProxyScopeCB($, TagClick(nodeId))
    }

    /** Скроллинг в списке тегов. */
    private def _onTagsListScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      dispatchOnProxyScopeCB($, TagsScroll(scrollTop, scrollHeight))
    }


    private val _tagsRenderF = { tagsProxy: ModelProxy[MTagsSearchS] =>
      val scCss = getScCssF()
      val TabCSS = scCss.Search.Tabs.TagsTag

      val tagsS = tagsProxy.value
      val _tagRow = TabCSS.tagRow: TagMod
      val _odd = TabCSS.oddRow: TagMod
      val _even = TabCSS.evenRow: TagMod

      <.div(
        TabCSS.tagsList,

        // Подписка на события скроллинга:
        ReactCommonUtil.maybe(tagsS.hasMoreTags && !tagsS.tagsReq.isPending) {
          ^.onScroll ==> _onTagsListScroll
        },

        // Рендер нормального списка найденных тегов.
        tagsS.tagsReq.render { tags =>
          if (tags.isEmpty) {
            <.div(
              _tagRow,
              Messages( MsgCodes.`No.tags.here` )
            )
          } else {
            tags
              .iterator
              .zipWithIndex
              .toVdomArray { case (mtag, i) =>
                // Рендер одного ряда.
                <.div(
                  _tagRow,

                  // Используем nodeId как ключ. Контроллер должен выверять отсутствие дубликатов в списке тегов.
                  ^.key := mtag.props.nodeId,

                  // Визуально разделять разные ряды.
                  if (i % 2 ==* 0) _odd else _even,

                  // Подсвечивать текущие выделенные теги.
                  ReactCommonUtil.maybe(tagsS.selectedId contains mtag.props.nodeId) {
                    TabCSS.selected
                  },

                  ^.onClick --> _onTagClick(mtag.props.nodeId),

                  mtag.props.hint.whenDefined
                )
              }
          }

        },

        // Рендер крутилки ожидания.
        tagsS.tagsReq.renderPending { _ =>
          <.div(
            _tagRow,
            LkPreLoaderR.AnimSmall
          )
        },

        // Рендер ошибки.
        tagsS.tagsReq.renderFailed { ex =>
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

      ) // tagsList

    }

    def render(s: State): VdomElement = {
      val scCss = getScCssF()
      val TabCSS = scCss.Search.Tabs.TagsTag

      <.div(
        TabCSS.outer,

        <.div(
          TabCSS.wrapper,

          <.div(
            TabCSS.inner,

            // Наконец, начинается содержимое вкладки с тегами:
            // TODO Надо контейнер отделить от динамической части. Тут по сути проброс в Props
            s.tagsC { _tagsRenderF.apply }

          ) // inner
        )   // wrap
      )     // outer
    }       // render()

  }


  val component = ScalaComponent.builder[Props]("Tags")
    .initialStateFromProps { tagSearchProxy =>
      State(
        tagsC = tagSearchProxy.connect(identity)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(tagSearchProxy: Props) = component(tagSearchProxy)

}
