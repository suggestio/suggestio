package io.suggest.ad.form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.05.15 18:41
 * Description: Константы формы создания/редактирования рекламной карточки, расшаренные между sjs и web21.
 */
object AdFormConstants {

  /** id контейнера поля загрузки фоновой картинки. */
  def BG_IMG_CONTAINER_ID       = "cardPhoto"

  /** id контейнера, который содержит в себе кнопки управления фоновой картинкой. */
  def BG_IMG_CONTROLS_CONTAINER_ID  = "blockImagebgImg"

  /** id поля, в котором задается ширина. */
  def WIDTH_INPUT_ID                = "afWidth"

  /** id поля, в котором задается длина. */
  def HEIGHT_INPUT_ID               = "afHeight"

  /** id поля, в котором лежит URL веб-сокета. */
  def WS_ID_INPUT_ID                = "wsId"

}
