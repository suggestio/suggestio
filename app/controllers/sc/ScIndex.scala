package controllers.sc

import _root_.util.{Context, PlayMacroLogsI}
import models.Context
import util.img.WelcomeUtil
import util.showcase._
import util.stat._
import util.acl._
import util.SiowebEsUtil.client
import controllers.routes
import io.suggest.model.EsModel.FieldsJsonAcc
import ShowcaseUtil._
import views.html.market.showcase._
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
trait ScIndexCommon extends ScController with PlayMacroLogsI with ScSiteConstants {

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

    def respJsonArgsFut: Future[FieldsJsonAcc] = {
      currAdnIdFut map { currAdnId =>
        List(
          "is_geo"      -> JsBoolean(isGeo),
          "curr_adn_id" -> currAdnId.fold[JsValue](JsNull){ JsString.apply }
        )
      }
    }

    def result: Future[Result] = {
      for {
        html      <- respHtmlFut
        jsonArgs  <- respJsonArgsFut
      } yield {
        // TODO Нужен аккуратный кеш тут. Проблемы с просто cache-control возникают, если список категорий изменился или
        // произошло какое-то другое изменение
        StatUtil.resultWithStatCookie {
          jsonOk("showcaseIndex", Some(html), acc0 = jsonArgs)
        }(ctx.request)
      }
    }

  }

}



/** Вспомогательная утиль для рендера indexTpl на нодах. */
trait ScIndexNodeCommon extends ScIndexCommon with ScIndexConstants {

  trait ScIndexNodeHelper extends ScIndexHelperBase {
    def adnNodeFut        : Future[MAdnNode]
    def spsrFut           : Future[AdSearch]
    def onCloseHrefFut    : Future[String]
    def geoListGoBackFut  : Future[Option[Boolean]]
    override lazy val currAdnIdFut: Future[Option[String]] = adnNodeFut.map(_.id)

    override def renderArgsFut: Future[ScRenderArgs] = {
      val _adnNodeFut = adnNodeFut
      val prodsStatsFut = _adnNodeFut.flatMap { adnNode =>
        MAd.findProducerIdsForReceiver(adnNode.id.get)
      }
      // Нужно собрать продьюсеров рекламы. Собираем статистику по текущим размещениям, затем грабим ноды.
      val prodsFut = prodsStatsFut flatMap { prodsStats =>
        val prodIds = prodsStats
          .iterator
          .filter { _._2 > 0 }
          .map { _._1 }
        MAdnNodeCache.multiGet(prodIds)
      } map { prodNodes =>
        prodNodes
          .map { adnNode => adnNode.id.get -> adnNode }
          .toMap
      }
      val (catsStatsFut, mmcatsFut) = {
        val f1 = _adnNodeFut
          .map { adnNode => getCats(adnNode.id) }
        f1.flatMap(_._1) -> f1.flatMap(_._2)
      }
      val waOptFut = _adnNodeFut
        .flatMap { adnNode => WelcomeUtil.getWelcomeRenderArgs(adnNode, ctx.deviceScreenOpt)(ctx) }
      val _onCloseHrefFut = onCloseHrefFut
      val _geoListGoBackFut = geoListGoBackFut
      val _spsrFut = spsrFut
      for {
        waOpt           <- waOptFut
        _catsStats      <- catsStatsFut
        prods           <- prodsFut
        _mmcats         <- mmcatsFut
        adnNode         <- _adnNodeFut
        _spsr           <- _spsrFut
        _onCloseHref    <- _onCloseHrefFut
        _geoListGoBack  <- _geoListGoBackFut
      } yield {
        new ScRenderArgs {
          override val searchInAdnId = {
            (adnNode.geo.allParentIds -- adnNode.geo.directParentIds)
              .headOption
              .orElse(adnNode.geo.directParentIds.headOption)
              .orElse(adnNode.id)
          }
          override val bgColor  = adnNode.meta.color getOrElse SITE_BGCOLOR_DFLT
          override val fgColor  = adnNode.meta.fgColor getOrElse SITE_FGCOLOR_DFLT
          override def name     = adnNode.meta.name
          override def mmcats   = _mmcats
          override def catsStats = _catsStats
          override def spsr     = _spsr
          override val onCloseHref = _onCloseHref
          override def logoImgOpt = adnNode.logoImgOpt
          override def shops    = prods
          override def geoListGoBack = _geoListGoBack
          override def welcomeOpt = waOpt
        }
      }
    }

  }

  trait ScIndexNodeSimpleHelper extends ScIndexNodeHelper {
    override def spsrFut = adnNodeFut map { adnNode =>
      AdSearch(
        levels      = List(AdShowLevels.LVL_START_PAGE),
        receiverIds = List(adnNode.id.get)
      )
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
    val helper = new ScIndexNodeSimpleHelper {
      override val geoListGoBackFut: Future[Option[Boolean]] = {
        MAdnNodeGeo.findIndexedPtrsForNode(adnId, maxResults = 1)
          .map { geos =>
            geos.headOption.map(_.glevel.isLowest)
          }
      }
      override def _reqArgs = args
      override val adnNodeFut = Future successful request.adnNode
      override def isGeo = false
      override implicit val _request = request
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

  /** Выдача для продьюсера, который сейчас админят. */
  def myAdsShowcase(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val helper = new ScIndexNodeHelper {
      // Тупо скопипасчено. Может быть тут ошибка:
      override def geoListGoBackFut = Future successful None

      /** При закрытии выдачи, админ-рекламодатель должен попадать к себе в кабинет. */
      override def onCloseHrefFut = {
        val url = Context.MY_AUDIENCE_URL + routes.MarketLkAdn.showAdnNode(adnId).url
        Future successful url
      }

      override def spsrFut = Future successful AdSearch( producerIds = List(adnId) )
      override def adnNodeFut = Future successful request.adnNode
      override def isGeo = false
      override implicit def _request = request
    }
    helper.result
  }

}

