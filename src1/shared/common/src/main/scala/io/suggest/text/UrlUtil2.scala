package io.suggest.text

import java.net.URI

import scalaz.{Validation, ValidationNel}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.10.17 20:48
  * Description: Кросс-платформенные UrlUtil.
  *
  * TODO Надо сюда перенести все UrlUtil из util.
  */
object UrlUtil2 {

  /** Валидация произвольного URL через Scalaz.
    *
    * @param url Ссылка для валидации.
    * @param err Код ошибки.
    * @return Результат валидации.
    */
  def validateUrl[E](url: String, err: => E): ValidationNel[E, String] = {
    // TODO Задействовать Validation.fromTryCatchThrowable(new URI(url).toString)
    try {
      val u = new URI(url)
      Validation.success( u.toString )
    } catch {
      case _: Throwable =>
        //LOGGER.debug("validateUrl(): Invalid URL: " + url, ex)
        Validation.failureNel(err)
    }
  }

}