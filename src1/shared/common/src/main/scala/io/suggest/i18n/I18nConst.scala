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

  /** Привести булёво значение к Yes или No.
    * И это потом можно в messages() передавать, для локализации ответа. */
  def yesNo(isYes: Boolean): String = {
    if (isYes)
      "Yes"
    else
      "No"
  }

}