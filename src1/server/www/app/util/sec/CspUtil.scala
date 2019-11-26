package util.sec

import javax.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.common.empty.OptionUtil
import io.suggest.proto.http.HttpConst
import io.suggest.sec.csp.{Csp, CspHeader, CspPolicy, CspViolationReport}
import models.mctx.ContextUtil
import play.api.Configuration
import play.api.mvc.Result
import util.cdn.CdnUtil
import play.api.libs.json._


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

  private val IFRAMES_SRCS: Set[String] = {
    // По идее, proto нужен только на dev, где всё по http. На prod будет https автоматом, т.к. там он везде.
    Set[String](
      Csp.Sources.BLOB,
      Csp.Sources.SELF,
      // Раньше было перечисление youtube и vimeo, но теперь всё проще: пусть просто будут все сайты.
      // Фильтрация по хостам должна быть где-то на сервере, а не в CSP.
      HttpConst.Proto.HTTPS + HttpConst.Proto.DELIM + Csp.Sources.*
    )
  }

  /** Включена ли поддержка CSP-заголовков? */
  private def IS_ENABLED = true

  /** CSP: Только репортить или репортить и запрещать вместе. */
  private def CSP_REPORT_ONLY = false

  /** Заголовок CSP, который можно модификацировать в контроллерах для разных нужд. */
  val CSP_DFLT_OPT: Option[CspHeader] = {
    OptionUtil.maybe( IS_ENABLED ) {
      // TODO На ноды балансируются только картинки, возможно ещё видео. И websocket'ы, связанные с ними. Надо ли их в common запихивать всё?
      val nodesWildcard = s"*.nodes.${contextUtil.HOST_PORT}"
      val commonSources = {
        // Т.к. сайт https-only, то игнорим протоколы, используем все CDN-хосты.
        val cdnHostsIter = cdnUtil.CDN_PROTO_HOSTS.valuesIterator.flatten
        val selfHosts = Csp.Sources.SELF :: Nil
        val selfNodes = nodesWildcard :: Nil
        // TODO Добавить cdn-nodes-домены сюда же.
        (cdnHostsIter ++ selfHosts ++ selfNodes)
          .toSet
      }

      val cdnNodes = for (rewriteFromTo <- cdnUtil.REWRITE_FROM_TO.view) yield {
        // Из ссылки вида -suggest.cdnvideo.ru получается *.cdnvideo.ru. Как-то это не очень хорошо.
        // *-suggest.cdnvideo.ru нельзя. Нельзя и s*-suggest.cdnvideo.ru. Поэтому разрешаем весь *.cdnvideo.ru:
        "*" + rewriteFromTo._2.replaceFirst("^[^\\.]*", "")
      }
      val commonSourcesWithInline = commonSources + Csp.Sources.UNSAFE_INLINE

      CspHeader(
        policy = CspPolicy(
          defaultSrc  = commonSources,
          imgSrc      = commonSources + Csp.Sources.DATA ++ cdnNodes,
          styleSrc    = commonSourcesWithInline,
          scriptSrc   = commonSourcesWithInline,
          // Коннекты: обычно, коннекты идут прямо на suggest.io. Для WebSocket надо явно прописать адреса из-за протокола.
          // Бывают XHR-коннекты через CDN, например GeoJSON для точек на карте выдачи.
          connectSrc  = {
            val wsProto = HttpConst.Proto.wsOrWss( contextUtil.HTTPS_ENABLED )
            commonSources ++ Seq(
              // Разрешить веб-сокеты в same-origin.
              s"$wsProto://${contextUtil.HOST_PORT}",
              s"$wsProto://$nodesWildcard"
            )
          },
          reportUri = Some( contextUtil.SC_URL_PREFIX + routes.Static.handleCspReport().url ),
          //frameSrc = VIDEO_SRCS,    // frameSrc is depreacted.
          childSrc = IFRAMES_SRCS,
          // default-src не распространяется на form-action:
          formAction = commonSources,
          // TODO На всякий случай, флешеапплеты разрешить только с youtube/video (или вообще запретить?).
          //objectSrc = Set( Csp.Sources.NONE )
          // service-worker: нужна задать собственный домен
          workerSrc = Set( Csp.Sources.SELF ),
        ),
        reportOnly = CSP_REPORT_ONLY
      )
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



  /** Готовые кастомные CSP-политики. */
  object CustomPolicies {

    /** CSP-заголовок сайта выдачи. Выдача нуждается в доступе к tile'ам карты. */
    def PageWithMapboxGl = mkCustomPolicyHdr { p0 =>
      val mbHost = "https://*.mapbox.com"
      val blob = Csp.Sources.BLOB

      p0.addDefaultSrc( blob )
        .addConnectSrc( mbHost )
        .addScriptSrc( blob, Csp.Sources.UNSAFE_EVAL )
        // Хз, надо ли imgSrc, т.к. она векторная и через XHR свои тайлы получает.
        .addImgSrc( mbHost, blob )
    }

    /** Страницы, которые содержат Leaflet-карту, живут по этой политике: */
    def PageWithOsmLeaflet = mkCustomPolicyHdr( _.allowOsmLeaflet )

    /** Страницы содержат Umap-карту на базе Leaflet. */
    def Umap = mkCustomPolicyHdr( _.allowUmap )

    /** Редактор карточек активно работает с "blob:", а quill-editor - ещё и с "data:" . */
    def AdEdit = mkCustomPolicyHdr { p0 =>
      val DATA = Csp.Sources.DATA
      p0.addImgSrc( Csp.Sources.BLOB, DATA )
        // Хз зачем, но добавление картинок в quill без этого ругается в логи (хотя и работает).
        .addConnectSrc( DATA )
        // Добавление видео производит манипуляции в DOM с видео.
        .addChildSrc( DATA )
    }

    def AdnEdit = mkCustomPolicyHdr { p0 =>
      p0.addImgSrc( Csp.Sources.BLOB )
    }

  }


  /** Поддержка JSON-парсинга полного JSON-отчёта CSP. */
  def WRAP_REPORT_READS: Reads[CspViolationReport] = {
    // Нежелательно .read[CspViolationReport]: если сделать implicit def, то будет бесконечная рекурсия.
    (__ \ "csp-report").read( CspViolationReport.REPORT_BODY_READS )
  }


  /** Контейнер неявных вещей и относящихся к ним. */
  object Implicits {

    implicit class ResultCspExt( result: Result ) {

      def withCspHeader(cspHeaderOpt: Option[(String, String)]): Result =
        cspHeaderOpt.fold(result)( result.withHeaders(_) )

    }

  }

}


/** Интерфейс поля с DI-инстансом [[CspUtil]]. */
trait ICspUtilDi {
  val cspUtil: CspUtil
}
