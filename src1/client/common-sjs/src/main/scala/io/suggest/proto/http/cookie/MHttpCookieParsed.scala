package io.suggest.proto.http.cookie

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.10.2020 16:38
  * Description: Модель распарсенного кукиса.
  */
object MHttpCookieParsed {

  object Words {
    final def EXPIRES = "Expires="
    final def MAX_AGE = "Max-Age="
    final def DOMAIN = "Domain="
    final def PATH = "Path="
    final def SECURE = "Secure"
    final def HTTP_ONLY = "HttpOnly"
    final def SAME_SITE = "SameSite="
  }

  @inline implicit def univEq: UnivEq[MHttpCookieParsed] = UnivEq.derive

}



final case class MHttpCookieParsed(
                                    name      : String,
                                    value     : String,
                                    attrs     : List[ICookieAttrToken]  = Nil,
                                  ) {
  import ICookieAttrToken._
  import MHttpCookieParsed._

  lazy val expires = attrs.collectFirst {
    case m: Expires => m.date
  }

  lazy val maxAge = attrs.collectFirst {
    case m: MaxAge => m.seconds
  }

  lazy val domain = attrs.collectFirst {
    case m: Domain => m.domain
  }

  lazy val path = attrs.collectFirst {
    case m: Path => m.path
  }

  lazy val secure = attrs contains Secure

  lazy val httpOnly = attrs contains HttpOnly

  lazy val sameSite = attrs.collectFirst {
    case m: SameSite => m.policy
  }

  private def _toCookieSb(sb: StringBuilder = new StringBuilder( value.length * 2 )): StringBuilder = {
    sb.append(name)
      .append('=')
      .append(value)
  }

  /** Рендер в Cookie-заголовок. */
  def toCookie: String =
    _toCookieSb().toString()

  /** Рендер в Set-Cookie-заголовок. */
  def toSetCookie: String = {
    val sb = _toCookieSb()

    // Отрендерить аттрибуты:
    for (attr <- attrs) {
      sb.append(';')
        .append(' ')

      attr match {
        case Expires(dt) =>
          sb.append( Words.EXPIRES )
            .append( DateTimeFormatter.RFC_1123_DATE_TIME.format(dt) )
        case MaxAge(seconds ) =>
          sb.append( Words.MAX_AGE )
            .append( seconds )
        case Domain(domain) =>
          sb.append( Words.DOMAIN )
            .append( domain )
        case Path(path) =>
          sb.append( Words.PATH )
            .append( path )
        case Secure =>
          sb.append( Words.SECURE )
        case HttpOnly =>
          sb.append( Words.HTTP_ONLY )
        case SameSite(policy) =>
          sb.append( Words.SAME_SITE )
            .append( policy )
      }
    }

    sb.toString()
  }

  override def toString: String = {
    StringUtil.toStringHelper(this, 16) { renderF =>
      val render0 = renderF("")
      render0( name )
      // value не рендерим: содежит данные идентификации
      if (attrs.nonEmpty)
        render0( attrs.mkString(",") )
    }
  }

}


sealed trait ICookieAttrToken

object ICookieAttrToken {

  @inline implicit def univEq: UnivEq[ICookieAttrToken] = UnivEq.derive

  final case class Expires(date: OffsetDateTime) extends ICookieAttrToken
  final case class MaxAge(seconds: Long) extends ICookieAttrToken
  final case class Domain(domain: String) extends ICookieAttrToken
  final case class Path(path: String) extends ICookieAttrToken
  case object Secure extends ICookieAttrToken
  case object HttpOnly extends ICookieAttrToken
  final case class SameSite(policy: String) extends ICookieAttrToken

}
