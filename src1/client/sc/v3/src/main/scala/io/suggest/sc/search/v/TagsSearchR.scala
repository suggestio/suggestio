package io.suggest.sc.search.v

import diode.react.{ModelProxy, ReactConnectProxy}
import diode.react.ReactPot._
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.sc.search.m.{MTagsSearchS, TagClick}
import io.suggest.sc.styl.GetScCssF
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
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

                // Ничего не загружено вообще. В норме, юзер не должен видеть это.
                tagsS.tagsReq.renderEmpty {
                  <.div(
                    _tagRow,
                    MsgCodes.`Not.loaded`       // TODO Messages()
                  )
                },

                // Рендер нормального списка найденных тегов.
                tagsS.tagsReq.render { tagsResp =>
                  if (tagsResp.tags.isEmpty) {
                    <.div(
                      _tagRow,
                      MsgCodes.`No.tags.here`   // TODO Messages()
                    )
                  } else {
                    tagsResp.tags
                      .iterator
                      .zipWithIndex
                      .toVdomArray { case (mtag, i) =>
                        // Рендер одного ряда.
                        <.div(
                          _tagRow,

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
                  <.div(
                    _tagRow,
                    ^.`class` := Css.Colors.RED,
                    ex.getMessage
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
