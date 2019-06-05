package io.suggest.text

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.19 9:45
  * Description: Простые валидаторы, пригодные для сервера и клиента.
  */
object Validators {

  /** Поверхностная тривиальная проверка валидности написанного email'а.
    * Прежде всего, для client-side. На сервере можно сложные либы подключить.
    *
    * @param email Предлагаемый адрес email.
    * @return true, если строка похожа на email.
    */
  def isEmailValid(email: String): Boolean = {
    {
      // Поверхностная проверка формата. Согласно RFC, верификация валидности email-адреса очень замудрёная.
      "^.+@.+(\\.[^\\.]+)+$".r
        .pattern
        .matcher( email )
        .matches()
    } && {
      // Общая проверка длины. RFC 2821 species (section 4.5.3.1) specifies a local-part length of 64 and a domain length of 255.
      // бОльшая длина допускается, но может не поддерживаться на стороне ПО.
      (email.length <= (64 + 255))
    }
  }


  /** Валидация номера телефона.
    *
    * @param phone Введённый номер телефона.
    * @return true, если строка похожа на номер телефона.
    */
  def isPhoneValid(phone: String): Boolean = {
    """^\(*\+*[1-9]{0,3}\)*-*[1-9]{0,3}[-. /]*\(*[2-9]\d{2}\)*[-. /]*\d{3}[-. /]*\d{4} *e*x*t*\.* *\d{0,4}$"""
      .r
      .pattern
      .matcher( phone )
      .matches()
  }

}
