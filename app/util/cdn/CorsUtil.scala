package util.cdn

import akka.stream.Materializer
import com.google.inject.Inject
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.Future
import play.api.http.HeaderNames._
import play.api.Play.{configuration, current}

import scala.collection.JavaConversions._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.14 17:33
 * Description: Поддержка HTTP CORS для межсайтовых ресурсов.
 * @see [[https://gist.github.com/jeantil/7214962]]
 * @see [[http://ru.wikipedia.org/wiki/Cross-origin_resource_sharing]]
 */
object CorsUtil {

  /** Активен ли механизм CORS вообще? */
  val IS_ENABLED = configuration.getBoolean("cors.enabled") getOrElse true

  /** Включен ли доступ к preflight-запросам? */
  val CORS_PREFLIGHT_ALLOWED: Boolean = configuration.getBoolean("cors.preflight.allowed") getOrElse true

}


/** Статическая утиль, которая инициализируется только в случае [[CorsUtil]].IS_ENABLED. */
object CorsUtil2 {

  val allowOrigins: String = {
    // Макс один домен. Чтобы не трахаться с доменами, обычно достаточно "*".
    configuration.getString("cors.allow.origin") getOrElse "*"
  }

  def allowMethods = {
    configuration.getStringList("cors.allow.methods")
      .fold("GET") { _.mkString(", ") }
  }

  def allowHeaders = {
    configuration.getStringList("cors.allow.headers")
      .fold(Option.empty[String]) { hs => Some( hs.mkString(", ") ) }
  }

  def allowCreds = configuration.getBoolean("cors.allow.credentials")

  lazy val PREFLIGHT_CORS_HEADERS: Seq[(String, String)] = {
    var acc: List[(String, String)] = Nil
    val ao = allowOrigins
    if (!ao.isEmpty) {
      acc ::= ACCESS_CONTROL_ALLOW_ORIGIN -> ao
      val am = allowMethods
      if (!am.isEmpty)
        acc ::= ACCESS_CONTROL_ALLOW_METHODS -> am
      val ah = allowHeaders
      if (ah.isDefined)
        acc ::= ACCESS_CONTROL_ALLOW_HEADERS -> ah.get
      val ac = allowCreds
      if (ac.nonEmpty)
        acc ::= ACCESS_CONTROL_ALLOW_CREDENTIALS -> ac.get.toString
    }
    acc
  }

  val SIMPLE_CORS_HEADERS: Seq[(String, String)] = {
    var acc: List[(String, String)] = Nil
    val ao = allowOrigins
    if (!ao.isEmpty) {
      acc ::= ACCESS_CONTROL_ALLOW_ORIGIN -> ao
    }
    acc
  }

  /** На какие запросы навешивать CORS-allow хидеры? */
  val ADD_HEADERS_URL_RE: Regex = {
    configuration.getString("cors.allow.headers.for.url.regex")
      .getOrElse { "^/v?assets/" }
      .r
  }

  def isAppendAllowHdrsForRequest(rh: RequestHeader): Boolean = {
    SIMPLE_CORS_HEADERS.nonEmpty &&
      ADD_HEADERS_URL_RE.pattern.matcher(rh.uri).find
  }

}


/** Фильтр. Должен без проблем инициализироваться, когда application not started. */
class CorsFilter @Inject() (
  override implicit val mat: Materializer
) extends Filter {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    var fut = f(rh)
    // Добавить CORS-заголовки, если необходимо.
    if ( CorsUtil.IS_ENABLED && CorsUtil2.isAppendAllowHdrsForRequest(rh) ) {
      // Делаем замыкание независимым от окружающего тела apply(). scalac-2.12 оптимизатор сможет заменить такое синглтоном.
      fut = fut.map { resp =>
        val st = resp.header.status
        if (st >= 400 && st <= 599) {
          // Ошибки можно возвращать так, без этих хидеров.
          resp
        } else {
          // Успешный подходящий запрос, навешиваем хидеры.
          resp.withHeaders(CorsUtil2.SIMPLE_CORS_HEADERS: _*)
        }
      }
    }
    fut
  }

}
