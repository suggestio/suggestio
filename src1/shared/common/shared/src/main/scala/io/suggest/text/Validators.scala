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


  def normalizeEmail(email: String): String = {
    email
      .trim
      .toLowerCase()
  }

  def normalizePhoneNumber(phone: String): String = {
    // Нужно только цифры, и ничего кроме. И в начале - код страны.
    phone
      .trim
      // Удалить все не-цифры:
      .replaceAll("[^0-9]", "")
      // Если код страны "8", то заменить на 7.
      .replaceFirst("^8(\\d{10})$", "7$1")
  }


  /** Валидация номера телефона.
    *
    * @param phone Введённый номер телефона.
    * @return true, если строка похожа на номер телефона.
    */
  def isPhoneValid(phone: String): Boolean = {
    val norm = "[^0-9]".r.replaceAllIn(phone, "")
    val l = norm.length
    (l >= 11) && (l <= 14)
  }


  /** Проверка валидности формата смс-кода, введённого юзером.
    *
    * @param smsCode Код из смс-сообщения.
    * @return true, если строка похожа на смс-код.
    */
  def isSmsCodeValid(smsCode: String): Boolean = {
    "^[0-9]{4,16}$".r
      .pattern
      .matcher(smsCode)
      .matches()
  }


  def isPasswordValid(password: String): Boolean = {
    password.length > 7
  }

}
