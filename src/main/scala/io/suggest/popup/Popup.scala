package io.suggest.popup

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.15 17:50
 * Description: Константы для попапов на сервере и клиенте.
 */
object PopupConstants {

  /** Минимальный сдвиг по вертикали при позиционировании. */
  def MIN_TOP_PX = 25

  /** id таргета для рендера попапов. */
  def CONTAINER_ID = "popupsContainer"

  /** Имя css-класса для покрытия фона оверлеем. */
  def OVERLAY_CSS_CLASS = "ovh"

  /** Какой-то флаг в списке class для идентификации связи попапа с javascript'ом. */
  def JS_POPUP_CLASS = "js-popup"

}
