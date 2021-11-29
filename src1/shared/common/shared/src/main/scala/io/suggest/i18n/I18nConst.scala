package io.suggest.i18n

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:15
  * Description: Пошаренные константы локализации.
  */
object I18nConst {

  /** Имя объекта lk-messages на стороне js. */
  final val JSMESSAGES_NAME = "_SioMessages"

  /** Глобальное имя на клиенте, в которое будет залита функция локализации. */
  def WINDOW_JSMESSAGES_NAME = "window." + JSMESSAGES_NAME

  def LANG_SUBMIT_FN = "lang"

  /** Name of language-code cookie.
    * Must be sync. with application.conf `play.i18n.langCookieName`. */
  def LANG_COOKIE_NAME = "l"

}
