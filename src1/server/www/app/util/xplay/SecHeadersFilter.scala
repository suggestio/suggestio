package util.xplay

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import io.suggest.sec.csp.Csp
import play.api.mvc.{Filter, RequestHeader, Result}
import util.acl.AclUtil
import util.sec.CspUtil

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
class SecHeadersFilterUtil {

  // TODO Нужно запилить CSP. Нужен report-only сначала, и модель с контроллером для приема отчетности.

  // Названия заголовков.
  val X_FRAME_OPTIONS_HEADER                    = "X-Frame-Options"
  val X_XSS_PROTECTION_HEADER                   = "X-XSS-Protection"
  val X_CONTENT_TYPE_OPTIONS_HEADER             = "X-Content-Type-Options"
  val X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER  = "X-Permitted-Cross-Domain-Policies"
  /** @see [[https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security]] */
  val STRICT_TRANSPORT_SECURITY                 = "Strict-Transport-Security"

  // Дефолтовые значения заголовков.
  val DEFAULT_FRAME_OPTIONS                     = "DENY"
  val DEFAULT_XSS_PROTECTION                    = "1; mode=block"
  val DEFAULT_CONTENT_TYPE_OPTIONS              = "nosniff"
  val DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES   = "master-only"

  // TODO Надо выставить хотя бы 12 недель, а лучше год, и флаг preload для доступа в hardcoded-списки браузеров.
  // Сейчас это пока не сделано, т.к. есть проблемы с letsEncrypt, nginx reload, других возможных проблем.
  //val DEFAULT_STRICT_TRANSPORT_SECURITY       = "max-age=10000; includeSubDomains"
  val DEFAULT_STRICT_TRANSPORT_SECURITY         = "max-age=10000"   // TODO Разрулить проблему сертификата с http://backend.suggest.io/


  // Хром не умеет в ALLOW-FROM. Надо разруливать через CSP frame-ancestors.
  def FRAMES_ALLOWED_FROM(url: String): Seq[(String, String)] = {
    val xfoHdr = X_FRAME_OPTIONS_HEADER -> ("ALLOW-FROM " + url)
    xfoHdr :: Nil
  }

}


/** play-фильтр для запросов и ответов, добавляющий http-заголовки политики безопасности. */
class SecHeadersFilter @Inject() (
                                   secHeadersFilterUtil       : SecHeadersFilterUtil,
                                   aclUtil                    : AclUtil,
                                   cspUtil                    : CspUtil,
                                   implicit private val ec    : ExecutionContext,
                                   override implicit val mat  : Materializer
                                 )
  extends Filter
{

  import secHeadersFilterUtil._


  /** Навесить на результат недостающие security-заголовки. */
  override def apply(f: (RequestHeader) => Future[Result])(rh0: RequestHeader): Future[Result] = {
    val rh = aclUtil.reqHdrFromRequestHdr( rh0 )
    val respFut = f(rh)

    val isSecure = rh.isTransferSecure

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

      // Добавить HSTS-хидер, если https. Для голого HTTP в этом нет смысла и необходимости.
      if (isSecure && !hs.contains(STRICT_TRANSPORT_SECURITY))
        acc ::= STRICT_TRANSPORT_SECURITY -> DEFAULT_STRICT_TRANSPORT_SECURITY

      // CSP: если включено, то Some.
      for {
        cspKv <- cspUtil.CSP_KV_DFLT_OPT
        // CSP: возможны два хидера: стандартный и report-only.
        if !(hs.contains(Csp.HDR_NAME) || hs.contains(Csp.HDR_NAME_REPORT_ONLY))
      } {
        acc ::= cspKv
      }

      resp.withHeaders(acc: _*)
    }
  }

}
