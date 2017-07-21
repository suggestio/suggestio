package io.suggest.i18n

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:15
  * Description: Пошаренные константы локализации.
  */
object I18nConst {

  /** Имя объекта lk-messages на стороне js. */
  final val MESSAGES_JSNAME = "_SioMessages"

  /** Глобальное имя на клиенте, в которое будет залита функция локализации. */
  def WINDOW_JSMESSAGES_NAME = "window." + MESSAGES_JSNAME

  /** Привести булёво значение к Yes или No.
    * И это потом можно в messages() передавать, для локализации ответа. */
  def yesNo(isYes: Boolean): String = {
    if (isYes)
      MsgCodes.`Yes`
    else
      MsgCodes.`No`
  }

}
