package controllers

import models.crawl.{ChangeFreqs, SiteMapUrl, SiteMapUrlT}
import models.stat.{ScStatAction, ScStatActions}
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import util.billing.StatBillingQueueActor
import util._
import util.acl._
import views.html.market._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: sio-market controller. Сюда попадают всякие экшены, которые относятся к маркету, но пока
 * не доросли до отдельных контроллеров.
 */

object Market extends SioController with SiteMapXmlCtl {

  /** Кол-во выводимых узлов размещения рекламы на главной. */
  val INDEX_NODES_LIST_LEN = configuration.getInt("market.index.nodes.list.len") getOrElse 5

  /** Юзер заходит в /m (или на market.suggest.io). Он видит страницу с описанием и кнопку для логина.
    * Если юзер уже залогинен и у него есть магазины/тц, то его надо переправить в ЛК. */
  /*def index = MarketIndexAccess { implicit request =>
    // Надо найти узлы, которые стоит отобразить на странице как точки для размещения рекламы.
    // Это должны быть не-тестовые ресиверы, имеющие логотипы.
    Ok(indexTpl(
      lf    = Some(Ident.emailPwLoginFormM),
      nodes = request.displayNodes
    ))
      .withHeaders(CACHE_CONTROL -> "public, max-age=300")
  }*/
  // Пока отключено визуальное отображение рекламных узлов и формы логина - тут в экшене это всё тоже выпилено:
  def index = MaybeAuthGet { implicit request =>
    cacheControlShort {
      Ok(indexTpl(
        lf = None,
        nodes = Nil
      ))
    }
  }

  /** Рендер верстки popup'а для отображения инфы по узлу. */
  // TODO Следует проверять права на узлы, чтобы не сканить.
  def adnNodePopup(adnId: String) = AdnNodePubMaybeAuth(adnId).apply { implicit request =>
    Ok(nodes._nodeInfoPopupTpl(request.adnNode))
  }


  // статистка

  /** Кем-то просмотрена одна рекламная карточка. */
  def adStats(martId: String, adId: String, actionRaw: String) = MaybeAuth.apply { implicit request =>
    val action: ScStatAction = ScStatActions.withName(actionRaw)
    MAd.getById(adId).map { madOpt =>
      madOpt.filter { mad =>
        mad.receivers.valuesIterator.exists(_.receiverId == martId)
      } foreach { mad =>
        StatBillingQueueActor.sendNewStats(rcvrId = martId, mad = mad, action = action)
        val adStat = new MAdStat(
          clientAddr  = request.remoteAddress,
          action      = action.toString(),
          ua          = request.headers.get(USER_AGENT),
          adIds       = Seq(adId),
          onNodeIdOpt = Some(martId),
          personId    = request.pwOpt.map(_.personId),
          isLocalCl   = false,
          adsRendered = 1
        )
        adStat.save
      }
    }
    NoContent
  }


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
    val textRenderOpt: Option[(HtmlFormat.Appendable, String)] = if (clangNorm startsWith "ru") {
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

