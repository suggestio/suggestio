package controllers

import models.crawl.{ChangeFreqs, SiteMapUrl, SiteMapUrlT}
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Call
import play.twirl.api.Html
import util.acl._
import views.html.market._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: sio-market controller. Сюда попадают всякие экшены, которые относятся к маркету, но пока
 * не доросли до отдельных контроллеров.
 */

object Market extends SioController with SiteMapXmlCtl {

  /**
   * Раздача страниц с текстами договоров sio-market.
   * @param clang Язык договора.
   * @return 200 Ok
   *         404 если текст договора не доступен на указанном языке.
   */
  def contractOfferText(clang: String) = MaybeAuth { implicit request =>
    import views.html.market.contract._
    val clangNorm = clang.toLowerCase.trim
    val ctx = implicitly[Context]
    val textRenderOpt: Option[(Html, String)] = if (clangNorm startsWith "ru") {
      val render = textRuTpl()(ctx)
      Some(render -> "ru")
    } else {
      None
    }
    textRenderOpt match {
      case Some((render, clang2)) =>
        val fullRender = contractBase(clang2)(render)(ctx)
        Ok(fullRender)
      case None =>
        http404ctx(ctx)
    }
  }


  /** Статическая страничка, описывающая суть sio market для владельцев WiFi. */
  def aboutMarket = MaybeAuth { implicit request =>
    // 2014.oct.22: Удаление старого about'а, который уже постарел совсем и потерял актуальность.
    //              Буклет о sio-маркете лежит по новой ссылке.
    MovedPermanently( routes.Market.marketBooklet().url )
  }

  /** Статическая страничка, описывающая суть sio market для рекламодателей. */
  def aboutForAdMakers = MaybeAuth { implicit request =>
    cacheControlShort {
      Redirect(routes.Market.marketBooklet().url)
    }
  }

  /** Выдать страницу с вертикальной страницой-презенташкой sio-маркета. */
  def marketBooklet = MaybeAuth { implicit request =>
    cacheControlShort {
      Ok(marketBookletTpl())
    }
  }

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

