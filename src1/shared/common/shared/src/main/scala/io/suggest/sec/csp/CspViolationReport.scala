package io.suggest.sec.csp

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.05.2020 15:32
  * Description: Модель отчёта о нарушении CSP-политики.
  */
object CspViolationReport {

  @inline implicit def univEq: UnivEq[CspViolationReport] = UnivEq.derive

  /** Поддержка JSON-парсинга для тела отчёта о нарушении CSP. */
  implicit def REPORT_BODY_READS: Reads[CspViolationReport] = (
    (__ \ "document-uri").read[String] and
    (__ \ "referrer").readNullable[String] and
    (__ \ "blocked-uri").readNullable[String] and
    (__ \ "violated-directive").read[String] and
    (__ \ "original-policy").read[String]
  )( apply _ )

}


/** Контейнер данных отчёта о нарушении безопасности.
  *
  * @param documentUri URL страницы.
  * @param referrer Реферер, если есть.
  * @param blockedUri Заблокированный запрос.
  * @param violatedDirective Нарушенная директива.
  * @param originalPolicy Политика безопасности.
  */
final case class CspViolationReport(
                                     documentUri        : String,
                                     referrer           : Option[String],
                                     blockedUri         : Option[String],
                                     violatedDirective  : String,
                                     originalPolicy     : String
                                   )
