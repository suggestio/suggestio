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

  val IS_ENABLED = configuration.getBoolean("cors.enabled") getOrElse false

  /** Включен ли доступ к preflight-запросам? */
  val CORS_PREFLIGHT_ALLOWED: Boolean = configuration.getBoolean("cors.preflight.allowed") getOrElse false

}


/** Статическая утиль, которая инициализируется только в случае [[CorsUtil]].IS_ENABLED. */
object CorsUtil2 {

  def isPreFlight(r: RequestHeader) = {
    r.method.equalsIgnoreCase("OPTIONS") && r.headers.get("Access-Control-Request-Method").nonEmpty
  }

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

}


/** Фильтр. Должен без проблем инициализироваться, когда application not started. */
class CorsFilter extends Filter {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    import CorsUtil._
    if (CorsUtil.IS_ENABLED) {
      import CorsUtil2._
      if (CORS_PREFLIGHT_ALLOWED && isPreFlight(rh)) {
        val result = if (CORS_PREFLIGHT_ALLOWED) {
          Results.Ok.withHeaders(PREFLIGHT_CORS_HEADERS : _*)
        } else {
          Results.NotFound
        }
        Future successful result
      } else {
        val fut = f(rh)
        val hs = SIMPLE_CORS_HEADERS
        if (hs.nonEmpty)
          fut.map { _.withHeaders(hs : _*) }
        else
          fut
      }
    } else {
      f(rh)
    }
  }

}
