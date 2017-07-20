package util.sec

import javax.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.sec.csp.{Csp, CspHeader, CspPolicy, CspViolationReport}
import models.mctx.ContextUtil
import play.api.Configuration
import play.api.mvc.Result
import util.cdn.CdnUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.05.17 16:12
  * Description: Серверная утиль для поддержки Content-Security-Policy.
  */
@Singleton
class CspUtil @Inject() (
                          cdnUtil        : CdnUtil,
                          configuration  : Configuration,
                          contextUtil    : ContextUtil
                        ) {

  val IS_ENABLED = configuration.getOptional[Boolean]("csp.enabled").getOrElse(true)

  /** Заголовок CSP, который можно модификацировать в контроллерах для разных нужд. */
  val CSP_DFLT_OPT: Option[CspHeader] = {
    if (IS_ENABLED) {

      /** CSP: Только репортить или репортить и запрещать вместе. */
      val CSP_REPORT_ONLY = configuration.getOptional[Boolean]("csp.report.only").getOrElse(true)   // TODO сделать по дефолту false.

      val commonSources = {
        // Т.к. сайт https-only, то игнорим протоколы, используем все CDN-хосты.
        val cdnHostsIter = cdnUtil.CDN_PROTO_HOSTS.valuesIterator.flatten
        val selfHosts = Csp.Sources.SELF :: Nil
        (cdnHostsIter ++ selfHosts)
          .toSet
      }
      val commonSourcesWithInline = commonSources + Csp.Sources.UNSAFE_INLINE
      val cspHdr = CspHeader(
        policy = CspPolicy(
          defaultSrc  = commonSources,
          imgSrc      = commonSources + Csp.Sources.DATA,
          styleSrc    = commonSourcesWithInline,
          scriptSrc   = commonSourcesWithInline,
          // Коннекты: обычно, коннекты идут прямо на suggest.io. Для WebSocket надо явно прописать адреса из-за протокола.
          // Бывают XHR-коннекты через CDN, например GeoJSON для точек на карте выдачи.
          connectSrc  = commonSources ++ Seq(
            // Разрешить веб-сокеты в same-origin.
            s"ws${if (contextUtil.HTTPS_ENABLED) "s" else ""}://${contextUtil.HOST_PORT}"
          ),
          reportUri = Some( routes.Static.handleCspReport().url )
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

  /** Отрендерить хидер для модифицировнной недефолтовой политики. */
  def mkCustomPolicyHdr(updatePolicyF: CspPolicy => CspPolicy): Option[(String, String)] = {
    CSP_DFLT_OPT
      .flatMap { csp0 =>
        val csp1 = csp0.withPolicy(
          updatePolicyF( csp0.policy )
        )
        csp1.headerOpt
      }
  }

  /** Накатить опциональный отрендеренный CSP-заголовок на HTTP-ответ. */
  def applyCspHdrOpt(hdrOpt: Option[(String, String)])(result: Result): Result = {
    hdrOpt.fold(result)( result.withHeaders(_) )
  }

  object CustomPolicies {

    /** CSP-заголовок сайта выдачи. Выдача нуждается в доступе к tile'ам карты. */
    val PageWithMapboxGl = mkCustomPolicyHdr( _.allowMapBoxGl )

    /** Страницы, которые содержат Leaflet-карту, живут по этой политике: */
    val PageWithOsmLeaflet = mkCustomPolicyHdr( _.allowOsmLeaflet )

  }


  import play.api.libs.json._
  import play.api.libs.functional.syntax._


  /** Поддержка JSON-парсинга для тела отчёта о нарушении CSP. */
  val REPORT_BODY_READS: Reads[CspViolationReport] = (
    (__ \ "document-uri").read[String] and
    (__ \ "referrer").readNullable[String] and
    (__ \ "blocked-uri").readNullable[String] and
    (__ \ "violated-directive").read[String] and
    (__ \ "original-policy").read[String]
  )( CspViolationReport.apply _ )


  /** Контейнер неявных вещей и относящихся к ним. */
  object Implicits {

    /** Поддержка JSON-парсинга полного JSON-отчёта CSP. */
    implicit val WRAP_REPORT_READS: Reads[CspViolationReport] = {
      (__ \ "csp-report").read( REPORT_BODY_READS )
    }

  }

}


/** Интерфейс поля с DI-инстансом [[CspUtil]]. */
trait ICspUtilDi {
  def cspUtil: CspUtil
}
