package util.cdn

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.util.logs.MacroLogsImplLazy
import models.mctx.ContextUtil

import scala.concurrent.ExecutionContext
import play.api.http.HeaderNames._
import japgolly.univeq._
import play.api.http.HttpVerbs

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
final class CorsUtil @Inject() (
                                 configuration: Configuration,
                                 contextUtil: ContextUtil
                               )
  extends MacroLogsImplLazy
{

  /** Активен ли механизм CORS вообще? */
  val IS_ENABLED: Boolean = configuration.getOptional[Boolean]("cors.enabled").getOrElseTrue

  /** Включен ли доступ к preflight-запросам? */
  val CORS_PREFLIGHT_ALLOWED: Boolean = configuration.getOptional[Boolean]("cors.preflight.allowed").getOrElseTrue

  def allowOrigins: String = {
    // Макс один домен. Чтобы не трахаться с доменами, обычно достаточно "*".
    configuration.getOptional[String]("cors.allow.origin").getOrElse("*")
  }

  def allowMethods = {
    configuration.getOptional[Seq[String]]("cors.allow.methods")
      .fold("*") { _.mkString(", ") }
  }

  def allowHeaders = {
    // Эти хидеры нужны для boopickle-общения через CDN. Т.е. бинарь подразумевает эти необычные хидеры:
    val hdrs0 = Set.empty + CONTENT_TYPE + ACCEPT
    val v = configuration.getOptional[Seq[String]]("cors.allow.headers")
      .fold( hdrs0 ) { hdrs0 ++ _ }
      .mkString(", ")
    Some(v)
  }

  def allowCreds = configuration.getOptional[Boolean]("cors.allow.credentials")

  val GET_CORS_HEADERS: List[(String, String)] = {
    var acc: List[(String, String)] = Nil
    val ao = allowOrigins
    if (!ao.isEmpty)
      acc ::= ACCESS_CONTROL_ALLOW_ORIGIN -> ao
    acc
  }

  lazy val PREFLIGHT_CORS_HEADERS: List[(String, String)] = {
    var acc: List[(String, String)] = GET_CORS_HEADERS
    if (acc.nonEmpty) {
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

  /** На какие запросы всегда навешивать CORS-allow хидеры? */
  private val ADD_HEADERS_URL_RE = "^/(v?assets/|~)".r


  /** Проверка хидеров на необходимость добавления CORS в ответ. */
  def isAppendAllowHdrsForRequest(rh: RequestHeader): Boolean = {
    GET_CORS_HEADERS.nonEmpty &&
    ADD_HEADERS_URL_RE.pattern.matcher(rh.uri).find
  }


  def withCorsHeaders(resp: Result)(implicit rh: RequestHeader) = {
    // Для GET-запросов достаточно коротких хидеров. Остальное требует указать allow-method и всё остальное.
    val corsHeaders = rh.method match {
      case HttpVerbs.GET  => GET_CORS_HEADERS
      case _              => PREFLIGHT_CORS_HEADERS
    }
    resp.withHeaders( corsHeaders: _* )
  }


  def withCorsIfNeeded(result: Result)(implicit rh: RequestHeader): Result = {
    val reqOriginOpt = rh.headers.get( ORIGIN )
    lazy val logPrefix = s"withCorsIfNeeded(${reqOriginOpt.getOrElse("")}):"

    (for {
      reqOrigin <- reqOriginOpt
      sioUrlPrefix = contextUtil.URL_PREFIX
      if {
        val r = reqOrigin ==* sioUrlPrefix
        if (!r) LOGGER.warn(s"$logPrefix Invalid/unexpected CORS Origin\n Origin: $reqOrigin\n Expected prefix: $sioUrlPrefix")
        r
      }
    } yield {
      // На продакшене - аплоад идёт на sX.nodes.suggest.io, а реквест из suggest.io - поэтому CORS тут участвует всегда.
      LOGGER.trace(s"$logPrefix Adding CORS-headers")
      withCorsHeaders( result )
    }) getOrElse {
      // В dev-режиме - отсутствие CORS - это норма. т.к. same-origin и для страницы, и для этого экшена.
      LOGGER.trace( s"Not adding CORS headers, missing/invalid Origin: ${rh.headers.get(ORIGIN).orNull}" )
      result
    }
  }

}


/** Фильтр. Должен без проблем инициализироваться, когда application not started. */
final class CorsFilter @Inject() (
                                   corsUtil                  : CorsUtil,
                                   implicit val ec           : ExecutionContext,
                                 )
  extends EssentialFilter
{

  override def apply(next: EssentialAction): EssentialAction = {
    EssentialAction { implicit rh =>
      val respFut0 = next(rh)
      // Надо ли добавлять CORS-заголовки?
      if ( corsUtil.IS_ENABLED && corsUtil.isAppendAllowHdrsForRequest(rh) ) {
        for (resp <- respFut0) yield {
          val st = resp.header.status
          if (st >= 400 && st <= 599) {
            // Ошибки можно возвращать так, без дополнительных CORS-хидеров.
            resp
          } else {
            // Успешный подходящий запрос, навешиваем хидеры.
            corsUtil.withCorsHeaders( resp )
          }
        }
      } else {
        // CORS-хидеры в ответе не требуются.
        respFut0
      }
    }
  }

}
