package util.cdn

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.n2.node.MNode
import io.suggest.util.logs.MacroLogsImplLazy
import models.mctx.{Context, ContextUtil}

import scala.concurrent.{ExecutionContext, Future}
import play.api.http.HeaderNames._
import japgolly.univeq._
import play.api.http.HttpVerbs
import util.acl.AclUtil
import util.domain.Domains3pUtil

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
                                 contextUtil: ContextUtil,
                                 domains3pUtil: Domains3pUtil,
                                 implicit private val ec: ExecutionContext,
                               )
  extends MacroLogsImplLazy
{

  /** Активен ли механизм CORS вообще? */
  val IS_ENABLED: Boolean = configuration.getOptional[Boolean]("cors.enabled").getOrElseTrue

  private val ALL = "*"
  private val VALUES_DELIM = ", "

  /** Simple GET requests CORS headers are minimal. */
  val GET_CORS_HEADERS: List[(String, String)] = {
    (ACCESS_CONTROL_ALLOW_ORIGIN -> ALL) ::  // TODO ALL -- Use Origin: value???
    Nil
  }

  private val POST_HEADERS_ALLOWED_ALWAYS = CONTENT_TYPE :: ACCEPT :: Nil

  /** Render one header tuple: Access-Control-Allow-Headers: ...
    *
    * @param allowXRequestedWith Only for valid 3p-domains.
    * @return
    */
  private def accessControlAllowHeaders_hdr(allowXRequestedWith: Boolean = false): (String, String) = {
    var hdrsAcc = POST_HEADERS_ALLOWED_ALWAYS

    if (allowXRequestedWith)
      hdrsAcc ::= X_REQUESTED_WITH

    ACCESS_CONTROL_ALLOW_HEADERS -> hdrsAcc.mkString(VALUES_DELIM)
  }

  private val PREFLIGHT_CORS_HEADERS_ALWAYS = {
    //(ACCESS_CONTROL_ALLOW_CREDENTIALS -> ALL) ::  // Need credentials access here?
    (ACCESS_CONTROL_ALLOW_METHODS -> ALL) ::
    GET_CORS_HEADERS
  }


  def preflightCorsHeaders(allowXRequestedWith: Boolean = false): List[(String, String)] = {
    accessControlAllowHeaders_hdr( allowXRequestedWith ) ::
    PREFLIGHT_CORS_HEADERS_ALWAYS
  }

  /** На какие запросы всегда навешивать CORS-allow хидеры? */
  private val ADD_HEADERS_URL_RE = "^/(v?assets/|~)".r


  /** Проверка хидеров на необходимость добавления CORS в ответ. */
  def isAppendAllowHdrsForRequest(rh: RequestHeader): Boolean = {
    ADD_HEADERS_URL_RE.pattern.matcher(rh.uri).find ||
    rh.headers.get( ORIGIN ).nonEmpty
  }


  def prepareCorsHeadersNo3p(allowXRequestedWith: Boolean = false)(implicit reqHdr: RequestHeader): List[(String, String)] = {
    // Для GET-запросов достаточно коротких хидеров. Остальное требует указать allow-method и всё остальное.
    reqHdr.method match {
      case HttpVerbs.GET  => GET_CORS_HEADERS
      // TODO POST from 3p-domains to suggest.io suffering from CORS: X-Requested-with header is not in Access-Control-Allow-Headers.
      case _              => preflightCorsHeaders( allowXRequestedWith )
    }
  }

  def prepareCorsHeaders(force: Boolean = false)(implicit ctx: Context): Future[List[(String, String)]] = {
    for (domainNode3pOpt <- ctx.domainNode3pOptFut) yield {
      prepareCorsHeadersForDomainOpt( domainNode3pOpt, force )(ctx.request)
    }
  }
  /** Async.prepare CORS headers for HTTP-request.
    * @return Nil if no cors headers is needed. */
  def prepareCorsHeadersForDomainOpt(domainNode3pOpt: Option[MNode], force: Boolean = false)
                                    (implicit request: RequestHeader): List[(String, String)] = {
    val reqOriginOpt = request.headers.get( ORIGIN )
    lazy val logPrefix = s"withCorsIfNeeded($force, ${reqOriginOpt.getOrElse("")}>${request.host}):"
    reqOriginOpt.fold {
      // No Origin: header - no CORS response-headers needed.
      List.empty[(String, String)]

    } { reqOrigin =>
      // Detect possible 3p-domain, if any.
        LOGGER.trace(s"$logPrefix domainNode3p=>${domainNode3pOpt.map(_.idOrNull).orNull}")
        if ({
          val r = {
            force ||
            (
              if (domainNode3pOpt.isEmpty)
                contextUtil.URL_PREFIX ==* reqOrigin
              else
                true // reqOrigin allowed because of known 3p-domain.
            ) ||
            (reqOrigin ==* "null") ||         // iOS 13.2.2 WKWebView шлёт очень необычное значение заголовка Origin.
            (reqOrigin startsWith "file://")
          } // Нельзя исключать внезапных чудес с изменением iOS null-значения в будущем.
          if (!r) LOGGER.warn(s"$logPrefix Invalid/unexpected CORS Origin\n Origin: $reqOrigin\n Expected prefix: ${domainNode3pOpt.fold(contextUtil.URL_PREFIX)(_ => reqOrigin)} force?$force")
          r
        }) {
          // На продакшене - аплоад идёт на sX.nodes.suggest.io, а реквест из suggest.io - поэтому CORS тут участвует всегда.
          // TODO 3p-domain: need to allow X-Requested-with header here.
          val corsHeaders = prepareCorsHeadersNo3p(
            allowXRequestedWith = domainNode3pOpt.nonEmpty,
          )
          LOGGER.trace(s"$logPrefix Will add ${corsHeaders.length} CORS-headers:\n ${corsHeaders.mkString("\n ")}")
          corsHeaders

        } else {
          // В dev-режиме - отсутствие CORS - это норма. т.к. same-origin и для страницы, и для этого экшена.
          LOGGER.trace( s"Not adding CORS headers, missing/invalid Origin: ${reqOriginOpt.orNull}" )
          Nil
        }
    }
  }

}


/** Фильтр. Должен без проблем инициализироваться, когда application not started. */
final class CorsFilter @Inject() (
                                   corsUtil                  : CorsUtil,
                                   aclUtil                   : AclUtil,
                                   contextUtil               : ContextUtil,
                                   implicit val ec           : ExecutionContext,
                                 )
  extends EssentialFilter
{

  override def apply(next: EssentialAction): EssentialAction = {
    EssentialAction { implicit rh =>
      val reqHdr = aclUtil.reqHdrFromRequestHdr( rh )
      val respFut0 = next( reqHdr )

      // Надо ли добавлять CORS-заголовки?
      if (
        corsUtil.IS_ENABLED &&
        corsUtil.isAppendAllowHdrsForRequest(rh)
      ) {
        respFut0.mapFuture { result =>
          // TODO Ensure if no CORS headers already presents in response?
          for {
            domain3pOpt <- contextUtil.domainNode3pOptFut( reqHdr )
          } yield {
            val corsHeaders = corsUtil.prepareCorsHeadersForDomainOpt( domain3pOpt )( reqHdr )
            result.withHeaders( corsHeaders: _* )
          }
        }

      } else {
        // CORS-хидеры в ответе не требуются.
        respFut0
      }
    }
  }

}
