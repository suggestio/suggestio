package util.showcase

import controllers.routes
import io.suggest.model.EsModel
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.ym.model.MAd
import models.crawl.{ChangeFreqs, SiteMapUrl, SiteMapUrlT}
import models.msc.ScJsState
import models.{AdSearch, Context}
import org.elasticsearch.common.unit.TimeValue
import org.joda.time.LocalDate
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.mvc.QueryStringBindable
import util.SiowebEsUtil.client
import util.seo.SiteMapXmlCtl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.11.14 17:37
 * Description: Выдача - основная составляющая sitemap'a. Тут система сбора карточек и отображения
 * карточек на sitemap'у.
 * Вычисление значений changefreq проходит по направлению оптимального кравлинга:
 * - Если изменялось сегодня, то hourly. Это простимулирует кравлер просмотреть документ сегодня.
 * - Если изменилось вчера или ранее, то значение lastmod уведомит кравлер, что страница изменилась.
*/
class ScSitemapsXml extends SiteMapXmlCtl {

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
    // Готовим неизменяемые потоко-безопасные константы, которые будут использованы для ускорения последующих шагов.
    val today = LocalDate.now()
    val qsb = ScJsState.qsbStandalone
    // Запускаем поточный обход всех опубликованных MAd'ов и поточную генерацию sitemap'а.
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
          .map[SiteMapUrlT] { mad2sxu(_, today, qsb) }
      }
    }
    Enumerator.flatten(erFut)
  }

  /**
   * Приведение рекламной карточки к элементу sitemap.xml.
   * @param mad Экземпляр рекламной карточки.
   * @param today Дата сегодняшнего дня.
   * @param ctx Контекст.
   * @return Экземпляр SiteMapUrl.
   */
  protected def mad2sxu(mad: MAd, today: LocalDate, qsb: QueryStringBindable[ScJsState])(implicit ctx: Context): SiteMapUrl = {
    val jsState = ScJsState(
      adnId           = mad.receivers.headOption.map(_._1),
      fadOpenedIdOpt  = mad.id,
      generationOpt   = None    // Всем юзерам поисковиков будет выдаваться одна ссылка, но всегда на рандомную выдачу.
    )
    val url = routes.MarketShowcase.geoSite().url + "#!?" + jsState.toQs(qsb)
    val lastDt = mad.dateEdited.getOrElse(mad.dateCreated)
    val lastDate = lastDt.toLocalDate
    SiteMapUrl(
      // TODO Нужно здесь перейти на #!-адресацию, когда появится поддержка этого чуда в js выдаче.
      loc = ctx.SC_URL_PREFIX + url,
      lastMod = Some( lastDate ),
      changeFreq = Some( if (lastDate isBefore today) ChangeFreqs.daily else ChangeFreqs.hourly )
    )
  }

}
