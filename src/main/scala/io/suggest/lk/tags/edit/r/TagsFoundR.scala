package io.suggest.lk.tags.edit.r

import diode.data.Pot
import io.suggest.common.tags.edit.TagsEditConstants.Search.Hints.ATTR_TAG_FACE
import diode.react.ModelProxy
import io.suggest.common.tags.edit.{MTagsFoundResp, TagsEditConstants}
import io.suggest.css.Css
import io.suggest.lk.tags.edit.vm.search.hints.SRow
import io.suggest.sjs.common.spa.DAction
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactEventH}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 18:52
  * Description: Компонент контейнера для найденных тегов.
  */
object TagsFoundR {

  type Props = ModelProxy[Pot[MTagsFoundResp]]


  protected class Backend($: BackendScope[Props, _]) {

    /** Коллбэк выбора найденного тега с помощью клика по нему в списке тегов. */
    def onTagFoundClick(e: ReactEventH): Option[Callback] = {
      for {
        srow    <- SRow.ofHtmlElUp(e.target)
        tagFace <- srow.tagFace
      } yield {
        // Тримминг здесь -- это обычно бесполезная операция. Но делается чисто на всякий случай, для самоуспокоения.
        val tagFace2 = tagFace.trim
        $.props >>= { p =>
          p.dispatchCB( AddTagFound(tagFace2) )
        }
      }
    }

    def render(p: Props) = {
      p().exists(_.tags.nonEmpty) ?= <.div(
        ^.`class`       := Css.HintList.CONTAINER,
        <.div(
          ^.`class`     := Css.HintList.OUTER,
          <.div(
            ^.`class`   := Css.HintList.CONTENT,
            ^.onClick   ==>? onTagFoundClick,

            // Отрендерить список найденных тегов
            for (tagFound <- p().get.tags) yield {
              <.div(
                ^.key := tagFound.name,
                ^.`class` := (Css.HintList.ROW + " " + TagsEditConstants.Search.Hints.HINT_ROW_CLASS),
                ATTR_TAG_FACE.reactAttr := tagFound.name,

                <.div(
                  ^.`class` := Css.NAME,
                  <.span(
                    ^.`class` := Css._PREFIX,
                    "#"
                  ),
                  tagFound.name
                ),
                <.div(
                  ^.value := Css.VALUE,
                  tagFound.count
                )
              )
            }

          )
        )
      )
    }

  }


  val component = ReactComponentB[Props]("TagsFound")
    .renderBackend[Backend]
    .build

  def apply(resp: Props) = component(resp)

}


case class AddTagFound(tagFace: String) extends DAction
