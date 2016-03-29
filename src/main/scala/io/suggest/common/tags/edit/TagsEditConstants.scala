package io.suggest.common.tags.edit

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

  /** id контейнера для списка найденных тегов. */
  def ADD_FOUND_TAGS_CONT_ID      = "tagAddFoundCont"

  /** На сколько миллисекунд откладывать запуск поискового запроса тегов. */
  def START_SEARCH_TIMER_MS       = 400

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

}
