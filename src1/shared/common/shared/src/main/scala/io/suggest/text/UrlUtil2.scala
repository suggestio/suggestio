package io.suggest.text

import java.net.URI

import io.suggest.common.html.HtmlConstants
import io.suggest.proto.http.HttpConst
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
      val u = URI.create(url)
      Validation.success( u.toString )
    } catch {
      case _: Throwable =>
        //LOGGER.debug("validateUrl(): Invalid URL: " + url, ex)
        Validation.failureNel(err)
    }
  }

  /** Префикс URL Hash. */
  def URL_HASH_PREFIX = "#!?"

  def getUrlHash(url: String): Option[String] = {
    val inx = url.indexOf(URL_HASH_PREFIX)
    if (inx < 0) {
      None
    } else {
      Some( url.substring(inx) )
    }
  }

  /** Выбрасывание из URL-хвоста лишнего префикса и прочего. */
  def clearUrlHash(urlHash: String): Option[String] = {
    val nonEmptyF = { s: String =>
      s.nonEmpty
    }
    Option(urlHash)
      .filter(nonEmptyF)
      .map(_.replaceFirst("^[#!?]+", ""))
      .filter(nonEmptyF)
  }


  /** Метод сборки абсолютной ссылки. */
  def mkAbsUrl(protoPrefix: String, secure: Boolean, relUrl: String): String = {
    val H = HttpConst.Proto
    if (relUrl.matches(s"^${H.HTTP}${H.SECURE_SUFFIX}?${H.DELIM}")) {
      // Уже абсолютная ссылка
      relUrl
    } else if (relUrl startsWith HttpConst.Proto.CURR_PROTO) {
      protoPrefix +
        (if(secure) HttpConst.Proto.SECURE_SUFFIX else "") +
        HttpConst.Proto.COLON +
        relUrl
    //} else if (relUrl startsWith HtmlConstants.SLASH) {
    // TODO Ссылка относительно корня сайта (/...). А что считать хостом и дефолтовым протоколом? Это должен бы js-роутер определять.
    } else {
      // Какая-то недоссылка. В норме сюда выполнение заходить не должно никогда.
      val d = HtmlConstants.COMMA + HtmlConstants.SPACE
      throw new IllegalArgumentException(protoPrefix + d + secure + d + relUrl)
    }
  }

}
