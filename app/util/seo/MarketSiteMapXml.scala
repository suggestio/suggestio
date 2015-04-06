package util.seo

import controllers.{routes, SioControllerUtil}
import models.Context
import models.crawl.{SiteMapUrlT, SiteMapUrl, ChangeFreqs}
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Call

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 22:13
 * Description: sitemap-утиль для информационного раздела /market/
 */
class MarketSiteMapXml extends SiteMapXmlCtl {

  /** Асинхронно поточно генерировать данные о страницах, подлежащих индексации. */
  override def siteMapXmlEnumerator(implicit ctx: Context): Enumerator[SiteMapUrlT] = {
    Enumerator[Call](
      routes.Market.marketBooklet()
    ) map { call =>
      SiteMapUrl(
        loc = ctx.SC_URL_PREFIX + call.url,
        lastMod = Some( SioControllerUtil.PROJECT_CODE_LAST_MODIFIED.toLocalDate ),
        changeFreq = Some( ChangeFreqs.weekly )
      )
    }
  }

}
