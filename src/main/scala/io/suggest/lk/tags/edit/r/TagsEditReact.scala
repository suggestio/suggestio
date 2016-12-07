package io.suggest.lk.tags.edit.r

import io.suggest.common.tags.edit.TagsEditConstants
import TagsEditConstants.Search.Hints.ATTR_TAG_FACE
import io.suggest.common.qs.QsConstants
import io.suggest.css.Css
import io.suggest.lk.tags.edit.m.ITagFound
import io.suggest.lk.vm.LkMessagesWindow.lkJsMessages
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventH, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 22:12
  * Description: Реализация редактора тегов на базе react.js.
  */
object TagsEditReact {


  object TagsEditor {

    /**
      * Настройки рендера.
      * @param onChange При изменении набора тегов вызывать эту функцию.
      */
    case class Props(
      onChange: () => Unit
    )

    /**
      * Класс состояния.
      * @param query Набираемое имя тега, и поисковый запрос одновременно.
      * @param tagsFound Список найденных тегов.
      * @param searhing Занята ли форма сейчас чем-либо?
      * @param tagsExists Список уже добавленных в заказ тегов.
      */
    case class State(
      query     : String          = "",
      tagsFound : Seq[ITagFound]  = Nil,
      searhing  : Boolean         = false,
      tagsExists: Set[String]     = Set.empty
    ) {
      def withQuery(query2: String) = copy(query = query2)
      def withTagsExists(tagsExists2: Set[String]) = copy(tagsExists = tagsExists2)
    }


    /** React-компонент редактора тегов. */
    class Backed($: BackendScope[Props, State]) {

      /**
        * Коллбэк ввода текста в поле имени нового тега.
        * Надо обновить состояние и запустить поисковый запрос, если требуется.
        */
      def onQueryChange(e: ReactEventI): Callback = {
        val query2 = e.target.value
        val cb0 = $.modState( _.withQuery(query2) )
        ???
      }


      /** Коллбэк выбора найденного тега с помощью клика по нему в списке тегов. */
      def onTagFoundClick(e: ReactEventH): Callback = {
        val tagFace = e.target.getAttribute( ATTR_TAG_FACE )
        ???
      }


      /** Клик по кнопке добавления тега. */
      def onAddBtnClick: Callback = {
        // Если поле пустое, то красным его подсветить. А если есть вбитый тег, то огранизовать добавление.
        ???
      }

      /** Клик по кнопке удаления exists-тега. */
      def onTagDeleteClick(tagName: String): Callback = {
        val sCb = $.modState { state0 =>
          state0.withTagsExists(
            state0.tagsExists - tagName
          )
        }
        sCb >> _doPropsOnChange
      }

      /** Дёрнуть props.onChange. */
      private def _doPropsOnChange: Callback = {
        for (props <- $.props) yield {
          props.onChange()
        }
      }


      /** Выполнить рендер редактора тегов. */
      def render(props: Props, state: State) = {
        <.div(
          // Локализованный заголовок виджета
          <.h2(
            ^.`class` := Css.Lk.MINOR_TITLE,
            lkJsMessages( "Tags.choosing" )
          ),

          // tagsAddForm: поле ввода имени тега.
          <.div(
            ^.`class` := Css.Input.INPUT_W,

            // Лэйбл для ввода названия тега
            <.label(
              ^.`class` := Css.Lk.LK_FIELD_NAME,
              lkJsMessages( "Add.tags" ),

              // Рендер инпута, содержащего искомое имя тега
              <.div(
                ^.`class` := (Css.Input.INPUT + " " + Css.CLEARFIX),
                <.div(
                  ^.`class` := (Css.Input.INPUT_SHADOW + " " + Css.Input.JS_INPUT_W),
                  <.input(
                    ^.`type`   := "text",
                    ^.name     := TagsEditConstants.ADD_TAGS_FN,
                    ^.value    := state.query,
                    ^.onChange ==> onQueryChange
                  )
                )
              )
            )
          ),

          // tagsAddFoundCont: поисковый контейнер с подсказками названий тегов.
          state.tagsFound.nonEmpty ?= <.div(
            ^.`class`       := Css.HintList.CONTAINER,
            <.div(
              ^.`class`     := Css.HintList.OUTER,
              <.div(
                ^.`class`   := Css.HintList.CONTENT,
                ^.onClick   ==> onTagFoundClick,

                // Отрендерить список найденных тегов
                for (tagFound <- state.tagsFound) yield {
                  <.div(
                    ^.`class` := Css.HintList.ROW,
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
          ),

          // tagAddBtn: Кнопка "добавить", изначально она пропада под списком подсказок
          <.input(
            ^.`class` := (Css.Buttons.BTN :: Css.Size.M :: Css.Buttons.MAJOR :: Nil).mkString(" "),
            ^.value   := lkJsMessages("Add"),
            ^.`type`  := "submit",
            ^.onClick --> onAddBtnClick
          ),

          // tagExistsCont: Уже добавленные к заказу гео-теги.
          state.tagsExists.nonEmpty ?= <.div(
            for {
              (tagName, i) <- state.tagsExists.toSeq.sorted.iterator.zipWithIndex
            } yield {
              <.div(
                ^.`class` := (Css.TagsEdit.JS_TAG_EDITABLE + " " + Css.TagsEdit.CONTAINER),
                // Инпут текушего тега
                <.input(
                  ^.`type` := "hidden",
                  ^.name   := ( TagsEditConstants.EXIST_TAGS_FN + QsConstants.KEY_PARTS_DELIM_STR + "[" + i + "]" + QsConstants.KEY_PARTS_DELIM_STR + TagsEditConstants.EXIST_TAG_NAME_FN),
                  ^.value  := tagName
                ),
                // Имя тега
                tagName,
                // Кнопка удаления тега из списка.
                <.span(
                  ^.`class` := (Css.TagsEdit.JS_TAG_DELETE :: Css.Buttons.BTN :: Css.Buttons.NEGATIVE :: Nil).mkString(" "),
                  ^.title   := lkJsMessages( "Delete" ),
                  ^.onClick --> onTagDeleteClick(tagName),
                  "[x]"
                )
              )
            }
          )

        )
      }

    }

  }

}
