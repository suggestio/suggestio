package util.xplay

import akka.stream.Materializer
import com.google.inject.{Inject, Singleton}
import io.suggest.sec.csp.{Csp, CspHeader, CspPolicy}
import models.mctx.ContextUtil
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}
import util.cdn.CdnUtil

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 11:43
 * Description: Для защиты от XSS и других видов атак используются специальные заголовки.
 * Этот фильтр расставляет необходимые заголовки в ответы.
 * Наверное эта логика потом будет перемещена на уровень action builder'ов, чтобы повысить гибкость системы.
 * @see [[https://www.playframework.com/documentation/2.3.3/SecurityHeaders]]
 */
@Singleton
class SecHeadersFilterUtil @Inject() (
                                       cdnUtil        : CdnUtil,
                                       configuration  : Configuration,
                                       contextUtil    : ContextUtil
                                     ) {

  // TODO Нужно запилить CSP. Нужен report-only сначала, и модель с контроллером для приема отчетности.

  // Названия заголовков.
  val X_FRAME_OPTIONS_HEADER                    = "X-Frame-Options"
  val X_XSS_PROTECTION_HEADER                   = "X-XSS-Protection"
  val X_CONTENT_TYPE_OPTIONS_HEADER             = "X-Content-Type-Options"
  val X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER  = "X-Permitted-Cross-Domain-Policies"
  //val CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy"

  // Дефолтовые значения заголовков.
  val DEFAULT_FRAME_OPTIONS                     = "DENY"
  val DEFAULT_XSS_PROTECTION                    = "1; mode=block"
  val DEFAULT_CONTENT_TYPE_OPTIONS              = "nosniff"
  val DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES   = "master-only"
  //val DEFAULT_CONTENT_SECURITY_POLICY = "default-src 'self'"

  def FRAMES_ALLOWED_FROM(url: String): Seq[(String, String)] = {
    val xfoHdr = X_FRAME_OPTIONS_HEADER -> ("ALLOW-FROM " + url)
    // TODO Нужна поддержка CSP. Хром не умеет в ALLOW-FROM. Надо CSP frame-ancestors.
    xfoHdr :: Nil
  }


  object Csp_ {

    val IS_ENABLED = configuration.getBoolean("csp.enabled").contains(true)

    /** Заголовок CSP, который можно модификацировать в контроллерах для разных нужд. */
    val CSP_DFLT_OPT: Option[CspHeader] = {
      if (IS_ENABLED) {
        /** CSP: Только репортить или репортить и запрещать вместе. */
        val CSP_REPORT_ONLY = configuration.getBoolean("csp.report.only").getOrElse(true)   // TODO сделать по дефолту false.

        val commonSources = {
          // Т.к. сайт https-only, то игнорим протоколы, используем все CDN-хосты.
          val cdnHostsIter = cdnUtil.CDN_PROTO_HOSTS.valuesIterator.flatten
          val selfHosts = Csp.Sources.SELF :: Nil
          (cdnHostsIter ++ selfHosts)
            .toSet
            .toList
        }
        val commonSourcesWithInline = Csp.Sources.UNSAFE_INLINE :: commonSources
        val cspHdr = CspHeader(
          policy = CspPolicy(
            defaultSrc  = commonSources,
            imgSrc      = Csp.Sources.DATA :: commonSources,
            styleSrc    = commonSourcesWithInline,
            scriptSrc   = commonSourcesWithInline,
            connectSrc  = contextUtil.HOST_PORT :: Nil
          ),
          reportOnly = CSP_REPORT_ONLY
        )

        Some(cspHdr)

      } else {
        None
      }
    }

    /** Отрендеренные название и значение HTTP-заголовка CSP. */
    val CSP_KV_DFLT_OPT: Option[(String, String)] = {
      CSP_DFLT_OPT
        .flatMap( _.headerOpt )
    }

  }

}


/** play-фильтр для запросов и ответов, добавляющий http-заголовки политики безопасности. */
class SecHeadersFilter @Inject() (
                                   secHeadersFilterUtil       : SecHeadersFilterUtil,
                                   implicit private val ec    : ExecutionContext,
                                   override implicit val mat  : Materializer
                                 )
  extends Filter
{

  import secHeadersFilterUtil._


  /** Навесить на результат недостающие security-заголовки. */
  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    val respFut = f(rh)

    for (resp <- respFut) yield {
      // Добавить только заголовки, которые отсутсвуют в исходнике.
      val hs = resp.header.headers
      var acc: List[(String, String)] = Nil

      if ( !hs.contains(X_FRAME_OPTIONS_HEADER) )
        acc ::= X_FRAME_OPTIONS_HEADER -> DEFAULT_FRAME_OPTIONS

      if ( !hs.contains(X_XSS_PROTECTION_HEADER) )
        acc ::= X_XSS_PROTECTION_HEADER -> DEFAULT_XSS_PROTECTION

      if ( !hs.contains(X_CONTENT_TYPE_OPTIONS_HEADER) )
        acc ::= X_CONTENT_TYPE_OPTIONS_HEADER -> DEFAULT_CONTENT_TYPE_OPTIONS

      if ( !hs.contains(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER) )
        acc ::= X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER -> DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES

      // CSP: если включено, то Some.
      for {
        cspKv <- secHeadersFilterUtil.Csp_.CSP_KV_DFLT_OPT
        // CSP: возможны два хидера: стандартный и report-only.
        if !(hs.contains(Csp.HDR_NAME) || hs.contains(Csp.HDR_NAME_REPORT_ONLY))
      } {
        acc ::= cspKv
      }

      resp.withHeaders(acc: _*)
    }
  }

}
