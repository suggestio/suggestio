package io.suggest.sec.csp

import io.suggest.common.empty.OptionUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.05.2020 15:27
  * Description: Заголовок CSP.
  *
  * @param policy Политика безопасности. Содержит значения хидера.
  * @param reportOnly Режим работы. Если true, значит всё разрешено, будут только report'ы. [false]
  */
final case class CspHeader(
                            policy      : CspPolicy,
                            reportOnly  : Boolean   = false
                          ) {

  def headerName: String = {
    if (reportOnly)
      Csp.CONTENT_SECURITY_POLICY_REPORT_ONLY
    else
      Csp.CONTENT_SECURITY_POLICY
  }

  def headerValue = policy.toString

  def headerOpt: Option[(String, String)] = {
    OptionUtil.maybe( policy.nonEmpty )(header)
  }

  def header = headerName -> headerValue

}


object CspHeader {
  @inline implicit def univEq: UnivEq[CspHeader] = UnivEq.derive

  def policy = GenLens[CspHeader]( _.policy )
}
