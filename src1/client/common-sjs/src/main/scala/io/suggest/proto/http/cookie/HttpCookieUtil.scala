package io.suggest.proto.http.cookie

import java.time.{Instant, OffsetDateTime, ZoneOffset}

import io.suggest.dt.CommonDateTimeUtil
import io.suggest.text.parse.ParserUtil.Implicits._

import scala.scalajs.js
import scala.util.parsing.combinator._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.09.2020 16:30
  * Description: Утиль для работы с кукисами внутри JS.
  * @see [[https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie]]
  */

object HttpCookieUtil {

  /** Разовый парсинг кукисов, для примера.
    *
    * @param cookieHeaderValue Строка-значение заголовка Cookie или Set-Cookie.
    * @return Распарсенный список кукисов или ошибку.
    */
  def parseCookies(cookieHeaderValue: String): Either[String, List[MHttpCookieParsed]] = {
    val parsers = new HttpCookieUtil {}
    parsers
      .parseAll( parsers.manyCookies, cookieHeaderValue )
      .toEither
  }


  /** Оценка кукиса, который получен на руки только что.
    * Discarding-куки содержит Expires в прошлом или неположительный max-age.
    *
    * @param cookieParsed Распарсенный кукиш.
    * @return true, если кукис актуален.
    *         false, если требует о кукисе с данным именем требуется забыть.
    *         None - transient-кукис, который живёт в текущих мыслях приложения, и не будет сохраняться никуда на постоянную.
    */
  def isOkOrDiscardCookie( cookieParsed: MHttpCookieParsed,
                           receivedAt: Option[Instant] ): Option[Boolean] = {
    cookieParsed
      .maxAge
      .map { maxAgeSec =>
        // Фильтровать по Max-Age относительно момента времени получения кукиса:
        maxAgeSec > 0 && receivedAt.fold(true) { rcvrAt =>
          val now = Instant.now()
          rcvrAt.plusSeconds( maxAgeSec ) isAfter now
        }
      }
      .orElse {
        for (cookieExpires <- cookieParsed.expires) yield {
          // Фильтруем по Expiers относительно now():
          cookieExpires isAfter OffsetDateTime.now()
        }
      }
  }

  /** Привести строку к регэкспу, игнорирующему регистр. */
  private def caselessRe(str: String) = ("(?i)" + str).r

}


/** Парсеры Http-кукисов. */
trait HttpCookieUtil extends JavaTokenParsers {

  import HttpCookieUtil._
  import ICookieAttrToken._
  import MHttpCookieParsed.Words


  /** <Cookie-name> can be any US-ASCII characters, except control characters, spaces, or tabs.
    * It also must NOT contain a separator character like the following:
    *   ( ) < > @ , ; : \ " / [ ] ? = { }.
    */
  val cName: Parser[String] =
    "(?i)[a-z0-9_.-]{1,50}".r


  /** A <cookie-value> can optionally be wrapped in double quotes and include any US-ASCII characters
    * excluding control characters, Whitespace, double quotes, comma, semicolon, and backslash.
    *
    * Encoding: Many implementations perform URL encoding on cookie values,
    * however it is not required per the RFC specification.
    * It does help satisfying the requirements about which characters are allowed for <cookie-value> though.
    */
  val cValue: Parser[String] = {
    // Выкидываем необязательные кавычки:
    val value: Parser[String] = "(?i)[~%.a-z0-9_+=/*&^#@$()<>-]{0,512}".r
    lazy val quoutedValue = {
      val quot: Parser[String] = "\""
      quot ~> value <~ quot
    }
    // Допускаем, что кавычек может не быть.
    value | quoutedValue
  }

  val partsDelim: Parser[String] = ";"

  /** The maximum lifetime of the cookie as an HTTP-date timestamp. */
  lazy val expires: Parser[Expires] = {
    // Используем js.Date для парсинга, т.к. ZonedDateTime тянет за собой всю базу часовых поясов
    caselessRe(Words.EXPIRES) ~> CommonDateTimeUtil.Regexp.rfc1123Dt ^^ { dtStr =>
      val millis1970 = js.Date.parse(dtStr)
      val inst = Instant
        .ofEpochSecond( (millis1970 / 1000).toLong )
        .atOffset( ZoneOffset.UTC )
      Expires( inst )
    }
  }


  /** Number of seconds until the cookie expires. A zero or negative number will expire the cookie immediately.
    * If both Expires and Max-Age are set, Max-Age has precedence. */
  lazy val maxAge = caselessRe(Words.MAX_AGE) ~> wholeNumber ^^ { maxAgeStr => MaxAge(maxAgeStr.toLong) }

  /** Host to which the cookie will be sent.
    * If omitted, defaults to the host of the current document URL, not including subdomains.
    * Leading dots in domain names (.example.com) are ignored.
    * If a domain is specified, then subdomains are always included.
    */
  lazy val domain = caselessRe(Words.DOMAIN) ~> "(?i)[a-z0-9.-]{3,64}".r ^^ Domain

  lazy val path = caselessRe(Words.PATH) ~> "(?i)/[a-z0-9%~:._/-]{0,70}".r ^^ Path

  lazy val secure = caselessRe(Words.SECURE) ^^^ Secure

  // Обязательно caseless, т.к. play шлёт "HTTPOnly" вместо стандартного "HttpOnly".
  lazy val httpOnly = caselessRe(Words.HTTP_ONLY) ^^^ HttpOnly

  lazy val sameSite = caselessRe(Words.SAME_SITE) ~> ("Strict" | "Lax" | "None") ^^ SameSite

  lazy val attrsDelimitedOpt: Parser[List[ICookieAttrToken]] = {
    val attrs = expires | maxAge | domain | path | secure | httpOnly | sameSite
    val delim = partsDelim
    lazy val attrsDelim = repsep( attrs, delim )
    opt(delim ~> attrsDelim) ^^ (_ getOrElse Nil)
  }

  /** Парсер одного кукиса. */
  lazy val oneCookie: Parser[MHttpCookieParsed] = {
    (cName ~ ("=" ~> cValue) ~ attrsDelimitedOpt) ^^ {
      case name ~ value ~ attrs =>
        MHttpCookieParsed(
          name  = name,
          value = value,
          attrs = attrs,
        )
    }
  }

  /** Парсер списка кукисов, разделённых запятыми.
    * Стандартный Fetch использует разделитель ", ".
    * cordova-fetch использует разделитель ",\n".
    */
  lazy val manyCookies: Parser[List[MHttpCookieParsed]] = {
    lazy val cookiesDelim = ","
    rep1sep( oneCookie, cookiesDelim )
  }

}
