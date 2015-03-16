package io.suggest.adv.ext.view

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.03.15 15:24
 * Description: Данные по странице с формами внешнего размещения карточки.
 */
object FormPage {

  /** id кнопки добавления новой цели. */
  def ID_ADD_TARGET_LINK = "socialApiAddTargetBtn"

  /** id общей формы со всеми выделенными целями, чтобы запустить на исполнение размещение. */
  def ID_ADV_FORM = "js-social-target-list"

  /** id контейнера, который содержит список форм-целей. */
  def ID_ALL_TARGETS_LIST = "eaTargets"

  /** Класс всех кнопок, занимающихся удалением целей с экрана и с сервера. */
  def CLASS_DELETE_TARGET_BTN = "js-delete-social-target"

  /** Класс формы редактирования одной цели размещения. */
  def CLASS_ONE_TARGET_FORM_INNER = "js-social_add-target-form"

  /** Контейнер данных по одной цели. */
  def CLASS_ONE_TARGET_CONTAINER  = "social-target"

  /** Класс редактируемых юзером полей формы одной цели. */
  def CLASS_ONE_TARGET_INPUT = "js-social-target_it"
}
