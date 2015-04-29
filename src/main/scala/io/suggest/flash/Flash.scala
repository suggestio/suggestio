package io.suggest.flash

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.15 17:03
 * Description:
 */
object FlashConstants {

  /** id контейнера, в котором отрендерены все уведомления, подлежащие отображению.
    * id используется для быстрого перехода к контейнеру уведомлений, чтобы не перебирать весь DOM. */
  def CONTAINER_ID          = "notify-flash-div"
  /** Имя css-класса все bar'ов уведомлений внутри контейнера. */
  def BAR_CLASS             = "status-bar"

  /** Имя data-аттрибута, храняещго состояние открытости одного (текущего) бара. */
  def IS_OPENED_DATA_ATTR   = "open"
  /** Значение open-аттрибута. */
  def OPENED_VALUE          = "1"

  /** Сколько миллисекунд надо отображать всплывшее отображение, перед тем как скрыть его. */
  def SHOW_TIMEOUT_MS       = 5000

  def SLIDE_DURATION_MS     = 400

}
