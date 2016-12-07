package io.suggest.lk.tags.edit.r

import io.suggest.common.tags.edit.TagsEditConstants
import TagsEditConstants.Search.Hints.ATTR_TAG_FACE
import io.suggest.common.qs.QsConstants
import io.suggest.css.Css
import io.suggest.lk.tags.edit.m.ITagFound
import io.suggest.lk.tags.edit.vm.search.hints.SRow
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.common.i18n.{IMessage, MMessage}
import io.suggest.sjs.common.model.Route
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactEventH, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 22:12
  * Description: Реализация редактора тегов на базе react.js.
  */
object TagsEditR {

  /**
    * Настройки рендера.
    * @param tagSearchRoute Роута для поиска тегов.
    * @param onChange При изменении набора тегов вызывать эту функцию.
    */
  case class Props(
    tagSearchRoute  : js.Dictionary[js.Any] => Route,
    onChange        : () => Unit
  )

  case class QueryState(
    text    : String = "",
    errors  : List[IMessage] = Nil
  )

  /**
    * Класс состояния.
    * @param query Набираемое имя тега, и поисковый запрос одновременно.
    * @param tagsFound Список найденных тегов.
    * @param searching Запущен поисковый запрос к серверу? Если да, то внутри timestamp запущенного реквеста.
    * @param tagsExists Список уже добавленных в заказ тегов.
    */
  case class State(
    query       : QueryState      = QueryState(),
    tagsFound   : Seq[ITagFound]  = Nil,
    searching   : Option[Long]    = None,
    tagsExists  : Set[String]     = Set.empty
  ) {
    def withQuery(query2: QueryState) = copy(query = query2)
    def withTagsExists(tagsExists2: Set[String]) = copy(tagsExists = tagsExists2)
  }


  /** React-компонент редактора тегов. */
  protected class Backend($: BackendScope[Props, State]) {

    /**
      * Коллбэк ввода текста в поле имени нового тега.
      * Надо обновить состояние и запустить поисковый запрос, если требуется.
      */
    def onQueryChange(e: ReactEventI): Callback = {
      val queryStr2 = e.target.value
      // TODO Запустить поисковый запрос к серверу и дальнейший рендер, когда есть буквы.
      $.modState { s0 =>
        s0.withQuery(
          s0.query.copy(
            text = queryStr2
          )
        )
      }
    }


    /** Коллбэк выбора найденного тега с помощью клика по нему в списке тегов. */
    def onTagFoundClick(e: ReactEventH): Option[Callback] = {
      for {
        srow    <- SRow.ofHtmlElUp(e.target)
        tagFace <- srow.tagFace
      } yield {
        // Тримминг здесь -- это обычно бесполезная операция. Но делается чисто на всякий случай, для самоуспокоения.
        val tagFace2 = tagFace.trim
        // Надо обновить состояние: скрыть плашку тегов, добавив выбранный тег в список уже добавленных.
        $.modState { s0 =>
          s0.copy(
            tagsFound  = Nil,
            tagsExists = s0.tagsExists + tagFace2
          )
        }
      }
    }


    /** Клик по кнопке добавления тега. */
    def onAddBtnClick: Callback = {
      val sCb = $.modState { s0 =>
        // Если поле пустое или слишком короткое, то красным его подсветить.
        // А если есть вбитый тег, то огранизовать добавление.
        val q = s0.query.text

        // Проверяем название тега...
        val errMsgOrNull = if (q.isEmpty) {
          MMessage( "error.required" )
        } else {
          // Проверяем минимальную длину
          val ql = q.length
          val minLen = TagsEditConstants.Constraints.TAG_LEN_MIN
          if (ql < minLen) {
            MMessage("error.minLength", minLen)
          } else {
            // Проверяем максимальную длину.
            val maxLen = TagsEditConstants.Constraints.TAG_LEN_MAX
            if (ql > maxLen) {
              MMessage("error.maxLength", maxLen)
            } else {
              null
            }
          }
        }
        val errMsgOpt = Option(errMsgOrNull)

        errMsgOpt.fold [State] {
          // Нет ошибок
          s0.copy(
            query = QueryState(),
            tagsExists = s0.tagsExists + q
          )
        } { merr =>
          // Есть какая-то ошибка, закинуть её в состояние.
          s0.withQuery(
            s0.query.copy(
              errors = merr :: Nil
            )
          )
        }
      }

      // Потом надо уведомить форму об изменениях, если форма успешно выполнила валидацию данных.
      sCb >> $.state >>= { s2 =>
        if (s2.query.errors.isEmpty) {
          _doPropsOnChange
        } else {
          Callback.empty
        }
      }
    }

    /** Клик по кнопке удаления exists-тега. */
    def onTagDeleteClick(tagName: String): Callback = {
      val sCb = $.modState { state0 =>
        state0.withTagsExists(
          state0.tagsExists - tagName
        )
      }
      // После рендера надо уведомить вышестояющую форму о наличии изменений.
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
      val SPACE = " "

      <.div(
        // Локализованный заголовок виджета
        <.h2(
          ^.`class` := Css.Lk.MINOR_TITLE,
          Messages( "Tags.choosing" )
        ),

        // tagsAddForm: поле ввода имени тега.
        <.div(
          ^.`class` := Css.Input.INPUT_W,

          // Лэйбл для ввода названия тега
          <.label(
            ^.`class` := Css.Lk.LK_FIELD_NAME,
            Messages( "Add.tags" ),

            // Рендер инпута, содержащего искомое имя тега
            <.div(
              ^.classSet1(
                Css.Input.INPUT + SPACE + Css.CLEARFIX,
                Css.Input.ERROR -> state.query.errors.nonEmpty
              ),
              <.div(
                ^.`class` := (Css.Input.INPUT_SHADOW + SPACE + Css.Input.JS_INPUT_W),
                <.input(
                  ^.`type`   := "text",
                  ^.name     := TagsEditConstants.ADD_TAGS_FN,
                  ^.value    := state.query.text,
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
              ^.onClick   ==>? onTagFoundClick,

              // Отрендерить список найденных тегов
              for (tagFound <- state.tagsFound) yield {
                <.div(
                  ^.key := tagFound.name,
                  ^.`class` := (Css.HintList.ROW + SPACE + TagsEditConstants.Search.Hints.HINT_ROW_CLASS),
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
          ^.`class` := (Css.Buttons.BTN :: Css.Size.M :: Css.Buttons.MAJOR :: Nil).mkString(SPACE),
          ^.value   := Messages("Add"),
          ^.`type`  := "submit",
          ^.onClick --> onAddBtnClick
        ),

        // tagExistsCont: Уже добавленные к заказу гео-теги.
        state.tagsExists.nonEmpty ?= <.div(
          for {
            (tagName, i) <- state.tagsExists.toSeq.sorted.iterator.zipWithIndex
          } yield {
            <.div(
              ^.`class` := (Css.TagsEdit.JS_TAG_EDITABLE + SPACE + Css.TagsEdit.CONTAINER),
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
                ^.`class` := (Css.TagsEdit.JS_TAG_DELETE :: Css.Buttons.BTN :: Css.Buttons.NEGATIVE :: Nil).mkString(SPACE),
                ^.title   := Messages( "Delete" ),
                ^.onClick --> onTagDeleteClick(tagName),
                "[x]"
              )
            )
          }
        )

      )
    }  // def render()

  } // class Backend


  protected val component = ReactComponentB[Props]("TagsEd")
    .initialState( State() )
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
