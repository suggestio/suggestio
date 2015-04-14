package util.xplay

import play.api.mvc.{Result, RequestHeader, Filter}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 11:43
 * Description: Для защиты от XSS и других видов атак используются специальные заголовки.
 * Этот фильтр расставляет необходимые заголовки в ответы.
 * Наверное эта логика потом будет перемещена на уровень action builder'ов, чтобы повысить гибкость системы.
 * @see [[https://www.playframework.com/documentation/2.3.3/SecurityHeaders]]
 */

object SecHeadersFilter {

  // TODO Нужно запилить CSP. Нужен report-only сначала, и модель с контроллером для приема отчетности.

  // Названия заголовков.
  val X_FRAME_OPTIONS_HEADER = "X-Frame-Options"
  val X_XSS_PROTECTION_HEADER = "X-XSS-Protection"
  val X_CONTENT_TYPE_OPTIONS_HEADER = "X-Content-Type-Options"
  val X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER = "X-Permitted-Cross-Domain-Policies"
  //val CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy"

  // Дефолтовые значения заголовков.
  val DEFAULT_FRAME_OPTIONS = "SAMEORIGIN"
  val DEFAULT_XSS_PROTECTION = "1; mode=block"
  val DEFAULT_CONTENT_TYPE_OPTIONS = "nosniff"
  val DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES = "master-only"
  //val DEFAULT_CONTENT_SECURITY_POLICY = "default-src 'self'"

  def apply() = new SecHeadersFilter

}


import SecHeadersFilter._


class SecHeadersFilter extends Filter {

  /** Навесить на результат недостающие security-заголовки. */
  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    val respFut = f(rh)
    respFut map { resp =>
      // Добавить только заголовки, которые отсутсвуют в исходнике.
      val hs = resp.header.headers
      var acc: List[(String, String)] = Nil
      if (!(hs contains X_FRAME_OPTIONS_HEADER))
        acc ::= X_FRAME_OPTIONS_HEADER -> DEFAULT_FRAME_OPTIONS
      if (!(hs contains X_XSS_PROTECTION_HEADER))
        acc ::= X_XSS_PROTECTION_HEADER -> DEFAULT_XSS_PROTECTION
      if (!(hs contains X_CONTENT_TYPE_OPTIONS_HEADER))
        acc ::= X_CONTENT_TYPE_OPTIONS_HEADER -> DEFAULT_CONTENT_TYPE_OPTIONS
      if (!(hs contains X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER))
        acc ::= X_PERMITTED_CROSS_DOMAIN_POLICIES_HEADER -> DEFAULT_PERMITTED_CROSS_DOMAIN_POLICIES
      resp.withHeaders(acc: _*)
    }
  }

}
