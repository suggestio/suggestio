package util.cdn

import play.api.mvc.{Results, Result, RequestHeader, Filter}
import scala.concurrent.Future
import play.api.http.HeaderNames._
import play.api.Play.{current, configuration}
import scala.collection.JavaConversions._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.14 17:33
 * Description: Поддержка HTTP CORS для межсайтовых ресурсов.
 * @see [[https://gist.github.com/jeantil/7214962]]
 * @see [[http://ru.wikipedia.org/wiki/Cross-origin_resource_sharing]]
 */
object CorsUtil {

  /** Включен ли доступ к preflight-запросам? */
  val CORS_PREFLIGHT_ALLOWED: Boolean = configuration.getBoolean("cors.preflight.allowed") getOrElse false

  def isPreFlight(r: RequestHeader) = {
    r.method.equalsIgnoreCase("OPTIONS") && r.headers.get("Access-Control-Request-Method").nonEmpty
  }

  def allowOrigins: String = {
    configuration.getStringList("cors.allow.origins")
      .map { _.mkString(" ") }
      .getOrElse {
        val sb = new StringBuilder
        CdnUtil.CDN_PROTO_HOSTS
          .iterator
          .filter(_._2.nonEmpty)
          .foreach { case (proto, hosts) =>
          hosts.foreach { host =>
            sb.append(proto)
              .append("://")
              .append(host)
              .append(' ')
          }
        }
        // Удалить лишний разделитель в конце строки.
        if (sb.nonEmpty)
          sb.setLength(sb.length - 1)
        sb.toString()
      }
  }

  def allowMethods = {
    configuration.getStringList("cors.allow.methods")
      .fold("GET") { _.mkString(", ") }
  }

  def allowHeaders = {
    configuration.getStringList("cors.allow.headers")
      .fold("") { _.mkString(", ") }
  }

  def allowCreds = {
    configuration.getBoolean("cors.allow.credentials")
      .fold("false") { _.toString }
  }

  val CORS_HEADERS: Seq[(String, String)] = {
    var acc: List[(String, String)] = Nil
    val ao = allowOrigins
    if (!ao.isEmpty) {
      acc ::= ACCESS_CONTROL_ALLOW_ORIGIN -> ao
      val am = allowMethods
      if (!am.isEmpty)
        acc ::= ACCESS_CONTROL_ALLOW_METHODS -> am
      val ah = allowHeaders
      if (!ah.isEmpty)
        acc ::= ACCESS_CONTROL_ALLOW_HEADERS -> ah
      val ac = allowCreds
      if (!ac.isEmpty)
        acc ::= ACCESS_CONTROL_ALLOW_CREDENTIALS -> ac
    }
    acc
  }

  /** Добавить CORS-заголовки к ответу. */
  def withCorsHeaders(res: Result): Result = {
    res.withHeaders(CORS_HEADERS : _*)
  }

}


import CorsUtil._


/** Фильтр. Должен без проблем инициализироваться, когда application not started. */
object CorsFilter extends Filter {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (isPreFlight(rh)) {
      val result = if (CORS_PREFLIGHT_ALLOWED) {
        withCorsHeaders(Results.Ok)
      } else {
        Results.NotFound
      }
      Future successful result
    } else {
      f(rh)
        .map { withCorsHeaders }
    }
  }

}
