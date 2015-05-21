package controllers.sc

import _root_.util.jsa.{SmRcvResp, Js}
import _root_.util.PlayMacroLogsI
import models.Context
import models.jsm.ScIndexResp
import models.msc._
import util.img.WelcomeUtil
import util.showcase._
import util.stat._
import util.acl._
import util.SiowebEsUtil.client
import controllers.routes
import views.html.sc._
import play.api.libs.json._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.mvc._
import play.api.Play, Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.14 12:12
 * Description: Экшены, генерирующие indexTpl выдачи для узлов вынесены сюда.
 */

/** Константы, используемые в рамках этого куска контроллера. */
trait ScIndexConstants {

  /** Кеш ответа showcase(adnId) на клиенте. */
  val SC_INDEX_CACHE_SECONDS: Int = configuration.getInt("market.showcase.index.node.cache.client.seconds") getOrElse 20

  /** Если true, то при наличии node.meta.site_url юзер при закрытии будет редиректиться туда.
    * Если false, то будет использоваться дефолтовый адрес для редиректа. */
  val ONCLOSE_HREF_USE_NODE_SITE = configuration.getBoolean("market.showcase.onclose.href.use.node.siteurl") getOrElse true

  /** Когда юзер закрывает выдачу, куда его отправлять, если отправлять некуда? */
  val ONCLOSE_HREF_DFLT = configuration.getString("market.showcase.onclose.href.dflt") getOrElse "http://yandex.ru/"

}



/** Общая утиль, испрользуемая в контроллере. */
trait ScIndexCommon extends ScController with PlayMacroLogsI {

  /** Базовый трейт для написания генератора производных indexTpl и ответов. */
  trait ScIndexHelperBase {
    def renderArgsFut: Future[ScRenderArgs]
    def isGeo: Boolean
    def currAdnIdFut: Future[Option[String]]
    def _reqArgs: ScReqArgs = ScReqArgs.empty
    implicit def _request: AbstractRequestWithPwOpt[_]
    lazy val ctx: Context = implicitly[Context]

    def respHtmlFut = {
      renderArgsFut
        .map { indexTpl(_)(ctx) }
    }
    def respHtmlJsFut = respHtmlFut.map(JsString(_))

    /** Кнопка навигации, которая будет отрендерена в левом верхнем углу indexTpl. */
    def _topLeftBtn: ScHdrBtn = ScHdrBtns.NavPanelOpen

    def respArgsFut: Future[ScIndexResp] = {
      val _currAdnIdOptFut = currAdnIdFut
      for {
        html          <- respHtmlJsFut
        currAdnIdOpt  <- _currAdnIdOptFut
      } yield {
        ScIndexResp(html, isGeo, currAdnIdOpt)
      }
    }

    /** Ответ для coffeescript-выдачи, там возвращался js. */
    protected def _result_v1(args: ScIndexResp): Future[Result] = {
      Ok(Js(65536, SmRcvResp(args)))
    }

    /** Ответ для sjs-выдачи, там нужен json. */
    protected def _result_v2(args: ScIndexResp): Future[Result] = {
      Ok(args.toJson)
    }

    protected def _resultVsn(args: ScIndexResp): Future[Result] = {
      _reqArgs.apiVsn match {
        case MScApiVsns.Coffee => _result_v1(args)
        case MScApiVsns.Sjs1   => _result_v2(args)
      }
    }
    
    def result: Future[Result] = {
      for {
        args <- respArgsFut
        res  <- _resultVsn(args)
      } yield {
        // TODO Нужен аккуратный кеш тут. Проблемы с просто cache-control возникают, если список категорий изменился или
        // произошло какое-то другое изменение
        StatUtil.resultWithStatCookie {
          res
        }(ctx.request)
      }
    }

  }

}



/** Вспомогательная утиль для рендера indexTpl на нодах. */
trait ScIndexNodeCommon extends ScIndexCommon with ScIndexConstants {

  /** Логика формирования indexTpl для конкретного узла. */
  trait ScIndexNodeHelper extends ScIndexHelperBase {
    def adnNodeFut        : Future[MAdnNode]
    def spsrFut           : Future[AdSearch]
    def onCloseHrefFut    : Future[String]
    def geoListGoBackFut  : Future[Option[Boolean]]
    override lazy val currAdnIdFut: Future[Option[String]] = adnNodeFut.map(_.id)

    // Нужно собрать продьюсеров рекламы. Собираем статистику по текущим размещениям, затем грабим ноды.
    /** Найти все id узлов-продьюсеров, как-то относящихся к текущей ноде как к ресиверу. */
    def prodsStatsFut = adnNodeFut.flatMap { adnNode =>
      MAd.findProducerIdsForReceiver(adnNode.id.get)
    }

    /** id найденных в [[prodsStatsFut()]] продьюсеров отмаппить на экземпляры узлов. */
    def prodsSeqFut = prodsStatsFut flatMap { prodsStats =>
      val prodIds = prodsStats
        .iterator
        .filter { _._2 > 0 }
        .map { _._1 }
      MAdnNodeCache.multiGet(prodIds)
    }

    /** Сделать карту продьюсеров. */
    def prodsFut = prodsSeqFut map { prodNodes =>
      prodNodes
        .map { adnNode => adnNode.id.get -> adnNode }
        .toMap
    }

    /** Получение данных по категориям: статистика и сами категории. */
    def getCatsResult: Future[GetCatsSyncResult] = {
      currAdnIdFut.flatMap { adnIdOpt =>
        ShowcaseUtil.getCats(adnIdOpt).future
      }
    }

    /** Получение карточки приветствия. */
    def welcomeAdOptFut: Future[Option[WelcomeRenderArgsT]] = {
      if (_reqArgs.withWelcomeAd) {
        adnNodeFut.flatMap { adnNode =>
          WelcomeUtil.getWelcomeRenderArgs(adnNode, ctx.deviceScreenOpt)(ctx)
        }
      } else {
        Future successful None
      }
    }

    def getAdnIdForSearchIn(adnNode: MAdnNode): Option[String] = {
      (adnNode.geo.allParentIds -- adnNode.geo.directParentIds)
        .headOption
        .orElse(adnNode.geo.directParentIds.headOption)
        .orElse(adnNode.id)
    }
    

    /** Приготовить аргументы рендера выдачи. */
    override def renderArgsFut: Future[ScRenderArgs] = {
      val _prodsFut = prodsFut
      val _onCloseHrefFut = onCloseHrefFut
      val _geoListGoBackFut = geoListGoBackFut
      val _spsrFut = spsrFut
      val _getCatsResult = getCatsResult
      val _searchInAdnIdFut = adnNodeFut.map(getAdnIdForSearchIn)
      for {
        waOpt           <- welcomeAdOptFut
        prods           <- _prodsFut
        GetCatsSyncResult(_catsStats, _mmCats) <- _getCatsResult
        adnNode         <- adnNodeFut
        _spsr           <- _spsrFut
        _onCloseHref    <- _onCloseHrefFut
        _geoListGoBack  <- _geoListGoBackFut
        _searchInAdnId  <- _searchInAdnIdFut
      } yield {
        import ShowcaseUtil._
        val _bgColor = adnNode.meta.color getOrElse SITE_BGCOLOR_DFLT
        val _fgColor = adnNode.meta.fgColor getOrElse SITE_FGCOLOR_DFLT
        new ScRenderArgs with ScReqArgsWrapper {
          override def reqArgsUnderlying = _reqArgs
          override def searchInAdnId  = _searchInAdnId
          override def bgColor        = _bgColor
          override def fgColor        = _fgColor
          override val hBtnArgs       = super.hBtnArgs
          override def topLeftBtn     = _topLeftBtn
          override def name           = adnNode.meta.name
          override def mmcats         = _mmCats
          override def catsStats      = _catsStats
          override def spsr           = _spsr
          override def onCloseHref    = _onCloseHref
          override def logoImgOpt     = adnNode.logoImgOpt
          override def shops          = prods
          override def geoListGoBack  = _geoListGoBack
          override def welcomeOpt     = waOpt
        }
      }
    }

  }

  /** Для обычной корневой выдачи обычно используется этот трейт вместо ScIndexNodeHelper. */
  trait ScIndexNodeSimpleHelper extends ScIndexNodeHelper {
    override def spsrFut = adnNodeFut map { adnNode =>
      new AdSearch {
        override def levels = List(AdShowLevels.LVL_START_PAGE)
        override def receiverIds = List(adnNode.id.get)
      }
    }

    override def onCloseHrefFut: Future[String] = adnNodeFut.map { adnNode =>
      adnNode.meta.siteUrl
        .filter { _ => ONCLOSE_HREF_USE_NODE_SITE }
        .getOrElse { ONCLOSE_HREF_DFLT }
    }
  }

}


/** Экшены для рендера indexTpl нод. */
trait ScIndexNode extends ScIndexNodeCommon {

  /** Базовая выдача для rcvr-узла sio-market. */
  def showcase(adnId: String, args: ScReqArgs) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    val _adnNodeFut = Future successful request.adnNode
    val helper = new ScIndexNodeSimpleHelper {
      override val geoListGoBackFut: Future[Option[Boolean]] = {
        MAdnNodeGeo.findIndexedPtrsForNode(adnId, maxResults = 1)
          .map { geos =>
            geos.headOption.map(_.glevel.isLowest)
          }
      }
      override def _reqArgs = args
      override def adnNodeFut = _adnNodeFut
      override def isGeo = false
      override implicit def _request = request
    }
    val resultFut = helper.result
    // собираем статистику, пока идёт подготовка результата
    val stat = ScIndexStatUtil(
      scSinkOpt = None,
      gsiFut    = args.geo.geoSearchInfoOpt,
      screenOpt = helper.ctx.deviceScreenOpt,
      nodeOpt   = Some(request.adnNode)
    )
    stat.saveStats onFailure { case ex =>
      LOGGER.warn(s"showcase($adnId): failed to save stats, args = $args", ex)
    }
    // Возвращаем результат основного действа. Результат вполне кешируем по идее.
    resultFut map { res =>
      res.withHeaders(CACHE_CONTROL -> s"public, max-age=$SC_INDEX_CACHE_SECONDS")
    }
  }

}

