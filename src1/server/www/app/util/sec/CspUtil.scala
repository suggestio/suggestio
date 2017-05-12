package util.sec

import com.google.inject.{Inject, Singleton}
import io.suggest.sec.csp.{Csp, CspHeader, CspPolicy}
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

  val IS_ENABLED = configuration.getBoolean("csp.enabled").getOrElse(true)

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
      }
      val commonSourcesWithInline = commonSources + Csp.Sources.UNSAFE_INLINE
      val cspHdr = CspHeader(
        policy = CspPolicy(
          defaultSrc  = commonSources,
          imgSrc      = commonSources + Csp.Sources.DATA,
          styleSrc    = commonSourcesWithInline,
          scriptSrc   = commonSourcesWithInline,
          connectSrc  = Set(
            // Разрешить XHR same-origin.
            contextUtil.HOST_PORT,
            // Разрешить веб-сокеты в same-origin.
            s"ws${if (contextUtil.HTTPS_ENABLED) "s" else ""}://${contextUtil.HOST_PORT}"
          )
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
    val ScSiteHdrOpt = mkCustomPolicyHdr( _.allowMapBoxGl )

    /** Страницы, которые содержат Leaflet-карту, живут по этой политике: */
    val PageWithOsmLeaflet = mkCustomPolicyHdr( _.allowOsmLeaflet )

  }

}


/** Интерфейс поля с DI-инстансом [[CspUtil]]. */
trait ICspUtilDi {
  def cspUtil: CspUtil
}
