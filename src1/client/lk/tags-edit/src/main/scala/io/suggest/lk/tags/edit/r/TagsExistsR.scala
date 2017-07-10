package io.suggest.lk.tags.edit.r

import diode.react.ModelProxy
import io.suggest.css.Css
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.i18n.MsgCodes
import io.suggest.lk.tags.edit.m.RmTag
import io.suggest.sjs.common.i18n.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 21:16
  * Description: Компонент списка текущих, добавленных в заказ, тегов.
  */
object TagsExistsR {

  type Props = ModelProxy[Set[String]]

  class Backend($: BackendScope[Props, Unit]) {

    /** Клик по кнопке удаления exists-тега. */
    def onTagDeleteClick(tagName: String): Callback = {
      dispatchOnProxyScopeCB($, RmTag(tagName))
    }


    def render(tagsExists: Props): VdomElement = {
      // tagExistsCont: Уже добавленные к заказу гео-теги.
      <.div(
        tagsExists().toSeq.sorted.toVdomArray { tagName =>
          <.div(
            ^.`class` := (Css.TagsEdit.JS_TAG_EDITABLE + SPACE + Css.TagsEdit.CONTAINER),
            ^.key     := tagName,

            // Имя тега
            tagName,

            // Кнопка удаления тега из списка.
            <.span(
              ^.`class`  := Css.flat(Css.TagsEdit.JS_TAG_DELETE, Css.Buttons.BTN, Css.Buttons.NEGATIVE),
              ^.title    := Messages( MsgCodes.`Delete` ),
              // TODO Брать tagName из key или содержимого div'а выше на уровне Callback'а, а не здесь.
              ^.onClick --> onTagDeleteClick(tagName),
              "[x]"
            )
          )
        }
      )
    }

  }

  val component = ScalaComponent.builder[Props]("TagsExist")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(tagsExists: Props) = component(tagsExists)

}


