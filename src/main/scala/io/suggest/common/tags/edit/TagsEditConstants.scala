package io.suggest.common.tags.edit

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 15:29
 * Description: Контейнер констант для редактора тегов.
 */
object TagsEditConstants {

  /** id контейнера уже созданных тегов. */
  def EXIST_TAGS_CONT_ID          = "tagsExistCont"

  /** id контейнера с инпутами для добавления нового тега. */
  def ADD_FORM_ID                 = "tagsAddForm"

  /** id инпута для ввода имени нового тега. */
  def ADD_NAME_INPUT_ID           = "tagAddNameInput"

  /** id кнопки добавления нового тега. */
  def ADD_BTN_ID                  = "tagAddBtn"

  /** На сколько миллисекунд откладывать запуск поискового запроса тегов. */
  def START_SEARCH_TIMER_MS       = 400

}
