package io.suggest.lk.tags.edit.r

import diode.react.ModelProxy
import io.suggest.css.Css
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 21:16
  * Description: Компонент списка текущих, добавленных в заказ, тегов.
  */
object TagsExistsR {

  type Props = ModelProxy[Set[String]]

  class Backend($: BackendScope[Props, _]) {

    /** Клик по кнопке удаления exists-тега. */
    def onTagDeleteClick(tagName: String): Callback = {
      $.props >>= { p =>
        p.dispatchCB( RmTag(tagName) )
      }
    }


    def render(tagsExists: Props) = {
      val SPACE = " "
      // tagExistsCont: Уже добавленные к заказу гео-теги.
      tagsExists().nonEmpty ?= <.div(
        for {
          (tagName, i) <- tagsExists().toSeq.sorted.iterator.zipWithIndex
        } yield {
          <.div(
            ^.`class` := (Css.TagsEdit.JS_TAG_EDITABLE + SPACE + Css.TagsEdit.CONTAINER),
            ^.key     := tagName,

            // Инпут текушего тега
            // TODO Не нужно наверное, удалить? Теги теперь через сериализацию состояния diode-формы передаются на сервер.
            /*<.input(
              ^.`type` := "hidden",
              ^.name   := ( TagsEditConstants.EXIST_TAGS_FN + QsConstants.KEY_PARTS_DELIM_STR + "[" + i + "]" + QsConstants.KEY_PARTS_DELIM_STR + TagsEditConstants.EXIST_TAG_NAME_FN),
              ^.value  := tagName
            ),*/

            // Имя тега
            tagName,

            // Кнопка удаления тега из списка.
            <.span(
              ^.`class`  := (Css.TagsEdit.JS_TAG_DELETE :: Css.Buttons.BTN :: Css.Buttons.NEGATIVE :: Nil).mkString(SPACE),
              ^.title    := Messages( "Delete" ),
              // TODO Брать tagName из key или содержимого div'а выше на уровне Callback'а, а не здесь.
              ^.onClick --> onTagDeleteClick(tagName),
              "[x]"
            )
          )
        }
      )
    }

  }

  val component = ReactComponentB[Props]("TagsExist")
    .renderBackend[Backend]
    .build

  def apply(tagsExists: Props) = component(tagsExists)

}

/** Акшен удаления тега из множества existing-тегов. */
case class RmTag(tagFace: String) extends DAction
