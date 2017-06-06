package util.cdn

import akka.stream.Materializer
import com.google.inject.{Inject, Singleton}
import controllers.routes
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import play.api.http.HeaderNames._

import scala.collection.JavaConversions._
import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.14 17:33
 * Description: Поддержка HTTP CORS для межсайтовых ресурсов.
  *
  * @see [[https://gist.github.com/jeantil/7214962]]
 * @see [[http://ru.wikipedia.org/wiki/Cross-origin_resource_sharing]]
 */
@Singleton
class CorsUtil @Inject() (
  configuration: Configuration
) {

  /** Активен ли механизм CORS вообще? */
  val IS_ENABLED = configuration.getBoolean("cors.enabled").getOrElse(true)

  /** Включен ли доступ к preflight-запросам? */
  val CORS_PREFLIGHT_ALLOWED: Boolean = configuration.getBoolean("cors.preflight.allowed").getOrElse(true)

  val allowOrigins: String = {
    // Макс один домен. Чтобы не трахаться с доменами, обычно достаточно "*".
    configuration.getString("cors.allow.origin") getOrElse "*"
  }

  def allowMethods = {
    configuration.getStringList("cors.allow.methods")
      .fold("GET") { _.mkString(", ") }
  }

  def allowHeaders = {
    // Эти хидеры нужны для boopickle-общения через CDN. Т.е. бинарь подразумевает эти необычные хидеры:
    val hdrs0 = Set( CONTENT_TYPE, ACCEPT )
    val v = configuration.getStringList("cors.allow.headers")
      .fold( hdrs0 ) { hdrs0 ++ _ }
      .mkString(", ")
    Some(v)
  }

  def allowCreds = configuration.getBoolean("cors.allow.credentials")

  lazy val PREFLIGHT_CORS_HEADERS: Seq[(String, String)] = {
    var acc: List[(String, String)] = Nil
    val ao = allowOrigins
    if (!ao.isEmpty) {
      acc ::= ACCESS_CONTROL_ALLOW_ORIGIN -> ao

      val am = allowMethods
      if (am.nonEmpty)
        acc ::= ACCESS_CONTROL_ALLOW_METHODS -> am

      for (ah <- allowHeaders )
        acc ::= ACCESS_CONTROL_ALLOW_HEADERS -> ah

      for (ac <- allowCreds)
        acc ::= ACCESS_CONTROL_ALLOW_CREDENTIALS -> ac.toString
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
      .getOrElse { "^/(v?assets/|~)" }
      .r
  }

  /** Проверка хидеров на необходимость добавления CORS в ответ. */
  def isAppendAllowHdrsForRequest(rh: RequestHeader): Boolean = {
    SIMPLE_CORS_HEADERS.nonEmpty && {
      val uri = rh.uri
      ADD_HEADERS_URL_RE.pattern.matcher(uri).find ||
        uri.startsWith(routes.Sc.renderMapNodesAll().url)
    }
  }

}


trait ICorsUtilDi {
  def corsUtil: CorsUtil
}


/** Фильтр. Должен без проблем инициализироваться, когда application not started. */
class CorsFilter @Inject() (
  corsUtil                  : CorsUtil,
  implicit val ec           : ExecutionContext,
  override implicit val mat : Materializer
)
  extends Filter
{

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    val fut0 = f(rh)
    // Добавить CORS-заголовки, если необходимо.
    if ( corsUtil.IS_ENABLED && corsUtil.isAppendAllowHdrsForRequest(rh) ) {
      // Делаем замыкание независимым от окружающего тела apply(). scalac-2.12 оптимизатор сможет заменить такое синглтоном.
      for (resp <- fut0) yield {
        val st = resp.header.status
        if (st >= 400 && st <= 599) {
          // Ошибки можно возвращать так, без дополнительных CORS-хидеров.
          resp
        } else {
          // Успешный подходящий запрос, навешиваем хидеры.
          resp.withHeaders(corsUtil.SIMPLE_CORS_HEADERS: _*)
        }
      }

    } else {
      // CORS-изменения в ответе не требуются.
      fut0
    }
  }

}
