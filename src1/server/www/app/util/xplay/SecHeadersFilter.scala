package util.xplay

import javax.inject.{Inject, Singleton}
import io.suggest.sec.csp.Csp
import play.api.http.ContentTypes
import play.api.mvc._
import util.acl.AclUtil
import util.sec.CspUtil

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

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

  // Сейчас это пока не сделано, т.к. есть проблемы с letsEncrypt, nginx reload, других возможных проблем.
  val DEFAULT_STRICT_TRANSPORT_SECURITY         = s"max-age=${180.days.toSeconds}"
  // TODO includeSubDomains - убедится в безопасности этой опции и добавить.
  // TODO preload для доступа в hardcoded-списки браузеров


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
                                   implicit private val ec    : ExecutionContext
                                 )
  extends EssentialFilter
{

  import secHeadersFilterUtil._

  override def apply(next: EssentialAction): EssentialAction = {
    EssentialAction.apply { rh0 =>
      val rh = aclUtil.reqHdrFromRequestHdr( rh0 )

      for (resp <- next(rh)) yield {
        // Добавить только заголовки, которые отсутсвуют в исходнике.
        val hs = resp.header.headers
        var acc: List[(String, String)] = Nil

        // TODO Opt Некоторые хидера нет смысла навешивать на не-html ответы. CSP включен только для html-страниц, но надо разобраться с остальными хидерами.
        // Это снизит траффик.

        val isHtml = resp.body.contentType.exists(_.startsWith( ContentTypes.HTML ))

        // Некоторые хидеры применимы только для html-страниц:
        if (isHtml) {

          // Фреймы. Наврядли фрейм-атаки актуальны для чего-то кроме веб-страниц.
          if ( !hs.contains(X_FRAME_OPTIONS_HEADER) )
            acc ::= X_FRAME_OPTIONS_HEADER -> DEFAULT_FRAME_OPTIONS

          // Только для IE8+, который его использует на веб-страницах. Малоактуально вообще, не только для вёб-страниц.
          if ( !hs.contains(X_XSS_PROTECTION_HEADER) )
            acc ::= X_XSS_PROTECTION_HEADER -> DEFAULT_XSS_PROTECTION

          // Только для всякого Adobe-мусора, которого в sio нет и быть не должно, поэтому выставляется только для html-страниц.
          if ( !hs.contains(X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER) )
            acc ::= X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER -> DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES

          // CSP, актуально только для html-страниц.
          for {
            cspKv <- cspUtil.CSP_KV_DFLT_OPT
            // CSP: возможны два хидера: стандартный и report-only. Проверить, что их ещё нет в обрабатываемом ответе:
            if !(hs.contains(Csp.CONTENT_SECURITY_POLICY) || hs.contains(Csp.CONTENT_SECURITY_POLICY_REPORT_ONLY))
          } {
            acc ::= cspKv
          }

        }

        val isSecure = rh.isTransferSecure

        // Добавить HSTS-хидер, если https. Для голого HTTP в этом нет смысла и необходимости.
        // Для любых ответов, даже для картинок. Это "заразный" хидер уровня целого сайта: пусть все знают, что suggest.io только httpS.
        if (isSecure && !hs.contains(STRICT_TRANSPORT_SECURITY))
          acc ::= STRICT_TRANSPORT_SECURITY -> DEFAULT_STRICT_TRANSPORT_SECURITY

        // Явный запрет авто-определние content-type браузером. Для любых ответов, content-type обязан быть валидным, либо должна быть ошибка.
        if ( !hs.contains(X_CONTENT_TYPE_OPTIONS_HEADER) )
          acc ::= X_CONTENT_TYPE_OPTIONS_HEADER -> DEFAULT_CONTENT_TYPE_OPTIONS

        resp.withHeaders(acc: _*)
      }
    }
  }

}
