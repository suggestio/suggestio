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

  /** Название поля, содержащего тип ws-сообщения. */
  def WS_MSG_TYPE_FN                = "type"

  /** Название поля с данными ws-сообщения. */
  def WS_MSG_DATA_FN                = "data"

  def TYPE_COLOR_PALETTE            = "colorPalette"

  /** css-класс для js-отработки цвета. */
  def CSS_JS_PALETTE_COLOR          = "js-color-block"

  /** id контейнера отрендеренной палитры. */
  def COLORS_DIV_ID                 = TYPE_COLOR_PALETTE

  // Названия корневых полей формы редактирования.
  def OFFER_K     = "offer"


  /** Константы для запуска детектора основных цветов картинок. */
  object ColorDetect {

    /** Размер генерируемой палитры. */
    def PALETTE_SIZE = 8

    /** Размер возвращаемой по WebSocket палитры. */
    def PALETTE_SHRINK_SIZE = 4

  }


  // v2: react-form

  /** id контейнера react-формы создания/редактирования рекламной карточки. */
  def AD_EDIT_FORM_CONT_ID = "aef"

}
