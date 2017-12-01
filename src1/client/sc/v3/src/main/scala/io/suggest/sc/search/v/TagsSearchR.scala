package io.suggest.sc.search.v

import diode.react.{ModelProxy, ReactConnectProxy}
import diode.react.ReactPot._
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.sc.search.m.{MTagsSearchS, TagClick}
import io.suggest.sc.styl.GetScCssF
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
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

    def render(tagSearchProxy: Props, s: State): VdomElement = {
      val scCss = getScCssF()
      val TabCSS = scCss.Search.Tabs.TagsTag

      <.div(
        TabCSS.outer,

        <.div(
          TabCSS.wrapper,

          <.div(
            TabCSS.inner,

            // Наконец, начинается содержимое вкладки с тегами:
            s.tagsC { tagsProxy =>
              val tagsS = tagsProxy.value
              val _tagRow = TabCSS.tagRow: TagMod
              val _odd = TabCSS.oddRow: TagMod
              val _even = TabCSS.evenRow: TagMod



              <.div(
                TabCSS.tagsList,

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
                          ^.key := mtag.nodeId,

                          // Визуально разделять разные ряды.
                          if (i % 2 ==* 0) _odd else _even,

                          // Подсвечивать текущие выделенные теги.
                          if (tagsS.selectedId contains mtag.nodeId)
                            TabCSS.selected
                          else
                            EmptyVdom,

                          ^.onClick --> _onTagClick(mtag.nodeId),

                          mtag.name
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

            } // tagsC tagsProxy

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
