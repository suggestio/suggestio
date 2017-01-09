package io.suggest.common.tags.edit

import io.suggest.common.html.HtmlConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 15:29
 * Description: Контейнер констант для редактора тегов.
 */
object TagsEditConstants {

  /** id контейнера уже созданных тегов. */
  def EXIST_CONT_ID               = "tagsExistCont"

  /** Название поля с названием нового добавляемого тега. */
  def ADD_TAGS_FN                 = "new"

  /** Название поля с уже связанными тегами. */
  def EXIST_TAGS_FN               = "tags"

  /** Имя поля с именем уже связанного тега. */
  def EXIST_TAG_NAME_FN           = "name"

  /** Имя поля с id узла связанного тега. */
  def EXIST_TAG_ID_FN             = "node_id"

  /** id контейнера с инпутами для добавления нового тега. */
  def ADD_FORM_ID                 = "tagsAddForm"

  /** id инпута для ввода имени нового тега. */
  def ADD_NAME_INPUT_ID           = "tagAddNameInput"

  /** id кнопки добавления нового тега. */
  def ADD_BTN_ID                  = "tagAddBtn"

  /** Класс-пометка для div'ов тегов в редакторе тегов. */
  def ONE_EXISTING_CONT_CLASS     = "js-tag-editable"

  /** Класс-пометка для элементов удаления в редакторе тегов. */
  def DELETE_EXISTING_CLASS       = "js-tag-delete"


  /** Константы для положительного ответа сервера по поводу добавления. */
  object ReplyOk {

    /** Имя поля с заново-отрендеренной формой. */
    def ADD_FORM_FN     = "f"

    /** Имя поля с новым отрендеренным списком тегов. */
    def EXIST_TAGS_FN   = "e"

  }


  /** Константы поиска тегов в редакторе. */
  object Search {

    /** id контейнера для списка найденных тегов. */
    def FOUND_TAGS_CONT_ID          = "tagAddFoundCont"

    /** На сколько миллисекунд откладывать запуск поискового запроса тегов. */
    def START_SEARCH_TIMER_MS       = 400

    /** Макс. кол-во тегов в ответе. */
    def LIVE_SEARCH_RESULTS_LIMIT   = 5

    /** Константы выпадающих подсказок поиска тегов в редакторе. */
    object Hints {

      /** Контейнер одного ряда-подсказки в списке рядов  */
      def HINT_ROW_CLASS = "js-shrow"

      def ATTR_TAG_FACE  = HtmlConstants.ATTR_PREFIX + HINT_ROW_CLASS

    }

  }

  /** Ограничения формы. */
  object Constraints {

    /** Максимальная символьная длина одного тега. */
    def TAG_LEN_MAX = 40

    /** Минимальная символьная длина одного тега. */
    def TAG_LEN_MIN = 1

    /** Сколько тегов юзер может добавить за один запрос максимум. */
    def TAGS_PER_ADD_MAX = 20

  }

}
