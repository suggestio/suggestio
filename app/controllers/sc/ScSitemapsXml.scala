package controllers.sc

import controllers.{routes, SiteMapXmlCtl}
import io.suggest.model.EsModel
import io.suggest.ym.model.MAd
import models.{ScJsState, AdSearch, Context}
import models.crawl.{SiteMapUrl, SiteMapUrlT}
import org.elasticsearch.common.unit.TimeValue
import play.api.libs.iteratee.Enumerator
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import io.suggest.util.SioEsUtil.laFuture2sFuture

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.11.14 17:37
 * Description: Выдача - основная составляющая sitemap'a. Тут система сбора карточек и отображения
 * карточек на sitemap'у.
 */
trait ScSitemapsXml extends ScController with SiteMapXmlCtl {

  /**
   * Асинхронно поточно генерировать данные о страницах выдачи, которые подлежат индексации.
   * Для этого нужно поточно пройти все отображаемые в выдаче карточки, сгенерив на их базе данные для sitemap.xml.
   * Кравлер может ждать ответа долго, а xml может быть толстая, поэтому у нас упор на легковесность
   * и поточность, а не на скорость исполнения.
   */
  override def siteMapXmlEnumerator(implicit ctx: Context): Enumerator[SiteMapUrlT] = {
    val someTrue = Some(true)
    val adSearch = new AdSearch {
      override def anyLevel = someTrue
      override def anyReceiverId = someTrue
    }
    var reqb = MAd.dynSearchReqBuilder(adSearch)
    reqb = MAd.prepareScrollFor(reqb)
    val erFut = reqb.execute().map { searchResp0 =>
      // Пришел ответ без результатов с начальным scrollId.
      Enumerator.unfoldM(searchResp0.getScrollId) { scrollId0 =>
        client.prepareSearchScroll(scrollId0)
          .setScroll(new TimeValue(EsModel.SCROLL_KEEPALIVE_MS_DFLT))
          .execute()
          .map { sr =>
            val scrollId = sr.getScrollId
            if (sr.getHits.getHits.length == 0) {
              client.prepareClearScroll().addScrollId(scrollId).execute()
              None
            } else {
              Some(scrollId, MAd.searchResp2list(sr))
            }
          }
      }.flatMap { mads =>
        // Из списка mads делаем Enumerator, который поочерёдно запиливает каждую карточку в элемент sitemap.xml.
        Enumerator(mads : _*)
          .map { mad2sxu }
      }
    }
    Enumerator.flatten(erFut)
  }

  /** Приведение рекламной карточки к элементу sitemap.xml. */
  protected def mad2sxu(mad: MAd)(implicit ctx: Context): SiteMapUrlT = {
    val jsState = ScJsState(
      adnId = mad.receivers.headOption.map(_._1),
      fadOpenedIdOpt = mad.id
    )
    val call = routes.MarketShowcase.syncGeoSite(jsState)
    SiteMapUrl(
      loc = ctx.currAudienceUrl + call.url
    )
  }

}
