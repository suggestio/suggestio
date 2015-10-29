package controllers.sc

import _root_.util.di._
import io.suggest.playx.ICurrentConf
import _root_.util.jsa.{SmRcvResp, Js}
import _root_.util.PlayMacroLogsI
import models.Context
import models.im.MImgT
import models.jsm.ScIndexResp
import models.msc.ScRenderArgs.ProdsLetterGrouped_t
import models.msc._
import play.twirl.api.Html
import util.acl._
import views.html.sc._
import play.api.libs.json._
import models._
import scala.concurrent.Future
import play.api.mvc._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.14 12:12
 * Description: Экшены, генерирующие indexTpl выдачи для узлов вынесены сюда.
 */

/** Константы, используемые в рамках этого куска контроллера. */
trait ScIndexConstants extends ICurrentConf {

  /** Кеш ответа showcase(adnId) на клиенте. */
  val SC_INDEX_CACHE_SECONDS: Int = configuration.getInt("market.showcase.index.node.cache.client.seconds") getOrElse 20

  /** Если true, то при наличии node.meta.site_url юзер при закрытии будет редиректиться туда.
    * Если false, то будет использоваться дефолтовый адрес для редиректа. */
  val ONCLOSE_HREF_USE_NODE_SITE = configuration.getBoolean("market.showcase.onclose.href.use.node.siteurl") getOrElse true

  /** Когда юзер закрывает выдачу, куда его отправлять, если отправлять некуда? */
  val ONCLOSE_HREF_DFLT = configuration.getString("market.showcase.onclose.href.dflt") getOrElse "http://yandex.ru/"

}



/** Общая утиль, испрользуемая в контроллере. */
trait ScIndexCommon
  extends ScController
  with PlayMacroLogsI
  with IStatUtil
{

  /** Базовый трейт для написания генератора производных indexTpl и ответов. */
  trait ScIndexHelperBase {
    def renderArgsFut: Future[ScRenderArgs]
    def isGeo: Boolean
    def currAdnIdFut: Future[Option[String]]
    def _reqArgs: ScReqArgs // = ScReqArgs.empty

    /** Фьючерс с определением достаточности имеющиейся геолокации для наилучшего определения узла. */
    def geoAcurrEnoughtFut: Future[Option[Boolean]]

    /** Предлагаемый заголовок окна выдачи, если возможно. */
    def titleOptFut: Future[Option[String]]

    /** Сборка значения для titleFut на основе имеющегося узла. */
    protected def _node2titleOpt(mnode: MNode): Option[String] = {
      val m = mnode.meta
      val title0 = m.basic.name
      val title2 = m.address.town.fold(title0)(townName => title0 + " (" + townName + ")")
      Some(title2)
    }

    implicit def _request: AbstractRequestWithPwOpt[_]

    /** Контейнер палитры выдачи. */
    def colorsFut: Future[IColors]

    lazy val ctx: Context = implicitly[Context]

    def respHtmlFut: Future[Html] = {
      renderArgsFut
        .map { indexTpl(_)(ctx) }
    }
    def respHtmlJsFut = respHtmlFut.map(JsString(_))


    def hBtnArgsFut: Future[HBtnArgs] = {
      colorsFut map { colors =>
        HBtnArgs(fgColor = colors.fgColor)
      }
    }

    /** Кнопка навигации, которая будет отрендерена в левом верхнем углу indexTpl. */
    def topLeftBtnHtmlFut: Future[Html] = {
      hBtnArgsFut map { _hBtnArgs =>
        val rargs = new ScReqArgsWrapper with IHBtnArgsFieldImpl {
          override def reqArgsUnderlying = _reqArgs
          override def hBtnArgs = _hBtnArgs
        }
        hdr._navPanelBtnTpl(rargs)
      }
    }

    def respArgsFut: Future[ScIndexResp] = {
      val _currAdnIdOptFut = currAdnIdFut
      for {
        html              <- respHtmlJsFut
        currAdnIdOpt      <- _currAdnIdOptFut
        geoAccurEnought   <- geoAcurrEnoughtFut
        titleOpt          <- titleOptFut
      } yield {
        ScIndexResp(
          html            = html,
          isGeo           = isGeo,
          currAdnId       = currAdnIdOpt,
          geoAccurEnought = geoAccurEnought,
          titleOpt        = titleOpt
        )
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
        statUtil.resultWithStatCookie {
          res
        }(ctx.request)
      }
    }

  }

}



/** Вспомогательная утиль для рендера indexTpl на нодах. */
trait ScIndexNodeCommon
  extends ScIndexCommon
  with ScIndexConstants
  with IWelcomeUtil
  with IScUtil
  with INodeCache
{

  /** Логика формирования indexTpl для конкретного узла. */
  trait ScIndexNodeHelper extends ScIndexHelperBase {
    def adnNodeFut        : Future[MNode]
    def spsrFut           : Future[AdSearch]
    def onCloseHrefFut    : Future[String]
    def geoListGoBackFut  : Future[Option[Boolean]]
    override lazy val currAdnIdFut: Future[Option[String]] = adnNodeFut.map(_.id)

    /** В рамках showcase(adnId) геолокация не требуется, узел и так известен. */
    override def geoAcurrEnoughtFut: Future[Option[Boolean]] = {
      Future successful None
    }

    /** Есть узел -- есть заголовок. */
    override def titleOptFut: Future[Option[String]] = {
      adnNodeFut
        .map { _node2titleOpt }
    }

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
      mNodeCache.multiGet(prodIds)
    }

    /** Сделать карту продьюсеров. */
    def prodsFut = prodsSeqFut map { prodNodes =>
      prodNodes
        .map { adnNode => adnNode.id.get -> adnNode }
        .toMap
    }

    override lazy val hBtnArgsFut = super.hBtnArgsFut

    /** Если узел с географией не связан, и есть "предыдущий" узел, то надо отрендерить кнопку "назад". */
    override def topLeftBtnHtmlFut: Future[Html] = {
      // Сразу запускаем сборку аргументов hbtn-рендера. Не здесь, так в super-классе понадобятся точно.
      val _hBtnArgsFut = hBtnArgsFut
      // В методе логика немного разветвляется и асинхронна внутри. false-ветвь реализована через Future.failed.
      val fut0 = if (_reqArgs.prevAdnId.nonEmpty) {
        Future successful None
      } else {
        Future failed new NoSuchElementException()
      }
      fut0.flatMap { _ =>
        adnNodeFut
      } filter { mnode =>
        // Продолжать только если текущий узел не связан с географией.
        val directGpIter = mnode.edges
          .withPredicateIter( MPredicates.GeoParent.Direct )
        directGpIter.isEmpty && {
          // Если в город (верхний узел) перешли из левого подузла, то у города НЕ должна отображаться кнопка "назад",
          // несмотря на отсутствие гео-родителей.
          val stiOpt = mnode.extras.adn
            .flatMap( _.shownTypeIdOpt )
            .flatMap( AdnShownTypes.maybeWithName )
          !stiOpt.exists( _.isTopLevel )
        }
      } flatMap { mnode =>
        // Надо рендерить кнопку возврата, а не дефолтовую.
        _hBtnArgsFut map { hBtnArgs0 =>
          val hBtnArgs2 = hBtnArgs0.copy(adnId = _reqArgs.prevAdnId)
          ScHdrBtns.Back2UpperNode(hBtnArgs2)
        }
      } recoverWith { case ex: Throwable =>
        if (!ex.isInstanceOf[NoSuchElementException])
          LOGGER.error("topLeftBtnHtmlFut(): Workarounding unexpected expection", ex)
        super.topLeftBtnHtmlFut
      }
    }

    /** Получение данных по категориям: статистика и сами категории. */
    def getCatsResult: Future[GetCatsSyncResult] = {
      currAdnIdFut.flatMap { adnIdOpt =>
        scUtil.getCats(adnIdOpt).future
      }
    }

    /** Получение карточки приветствия. */
    def welcomeAdOptFut: Future[Option[WelcomeRenderArgsT]] = {
      if (_reqArgs.withWelcomeAd) {
        adnNodeFut.flatMap { adnNode =>
          welcomeUtil.getWelcomeRenderArgs(adnNode, ctx.deviceScreenOpt)(ctx)
        }
      } else {
        Future successful None
      }
    }

    def getAdnIdForSearchIn(mnode: MNode): Option[String] = {
      val allParentIds = mnode.edges
        .withPredicateIterIds( MPredicates.GeoParent )
        .toSet
      val directParentIds = mnode.edges
        .withPredicateIterIds( MPredicates.GeoParent.Direct )
        .toSet
      (allParentIds -- directParentIds)
        .headOption
        .orElse(directParentIds.headOption)
        .orElse(mnode.id)
    }

    /** Для рендера списка магазинов требуется сгруппированный по первой букве список узлов. */
    def prodsLetterGroupedFut: Future[ProdsLetterGrouped_t] = {
      if (_reqArgs.apiVsn.withStaticNodeList) {
        prodsSeqFut.map {
          ScRenderArgs.groupNodesByNameLetter
        }
      } else {
        Future successful Nil
      }
    }

    /** Получение графического логотипа узла, если возможно. */
    def logoImgOptFut: Future[Option[MImgT]] = {
      for {
        currNode     <- adnNodeFut
        logoOptRaw   <- logoUtil.getLogoOfNode(currNode)
        logoOptScr   <- logoUtil.getLogoOpt4scr(logoOptRaw, _reqArgs.screen)
      } yield {
        logoOptScr
      }
    }


    /** Контейнер палитры выдачи. */
    override def colorsFut: Future[IColors] = {
      adnNodeFut map { adnNode =>
        val _bgColor = adnNode.meta.colors.bg.fold(scUtil.SITE_BGCOLOR_DFLT)(_.code)
        val _fgColor = adnNode.meta.colors.fg.fold(scUtil.SITE_FGCOLOR_DFLT)(_.code)
        Colors(bgColor = _bgColor, fgColor = _fgColor)
      }
    }

    /** Приготовить аргументы рендера выдачи. */
    override def renderArgsFut: Future[ScRenderArgs] = {
      val _onCloseHrefFut     = onCloseHrefFut
      val _geoListGoBackFut   = geoListGoBackFut
      val _spsrFut            = spsrFut
      val _getCatsResult      = getCatsResult
      val _prodsLetGrpFut     = prodsLetterGroupedFut
      val _adnNodeFut         = adnNodeFut
      val _logoImgOptFut      = logoImgOptFut
      val _colorsFut          = colorsFut
      val _hBtnArgsFut        = hBtnArgsFut
      val _topLeftBtnHtmlFut  = topLeftBtnHtmlFut
      for {
        waOpt           <- welcomeAdOptFut
        GetCatsSyncResult(_catsStats, _mmCats) <- _getCatsResult
        adnNode         <- _adnNodeFut
        _spsr           <- _spsrFut
        _onCloseHref    <- _onCloseHrefFut
        _geoListGoBack  <- _geoListGoBackFut
        _prodsLetGrp    <- _prodsLetGrpFut
        _logoImgOpt     <- _logoImgOptFut
        _colors         <- _colorsFut
        _hBtnArgs       <- _hBtnArgsFut
        _topLeftBtnHtml <- _topLeftBtnHtmlFut
      } yield {
        new ScRenderArgs with ScReqArgsWrapper with IColorsWrapper {
          override def tilesBgFillAlpha   = scUtil.TILES_BG_FILL_ALPHA
          override def reqArgsUnderlying  = _reqArgs
          override def _underlying        = _colors
          override def hBtnArgs           = _hBtnArgs
          override def topLeftBtnHtml     = _topLeftBtnHtml
          override def title              = adnNode.meta.basic.name
          override def mmcats             = _mmCats
          override def catsStats          = _catsStats
          override def spsr               = _spsr
          override def onCloseHref        = _onCloseHref
          override def logoImgOpt         = _logoImgOpt
          override def geoListGoBack      = _geoListGoBack
          override def welcomeOpt         = waOpt
          override def shopsLetterGrouped = _prodsLetGrp
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
      adnNode.meta
        .business
        .siteUrl
        .filter { _ => ONCLOSE_HREF_USE_NODE_SITE }
        .getOrElse { ONCLOSE_HREF_DFLT }
    }
  }

}


/** Экшены для рендера indexTpl нод. */
trait ScIndexNode
  extends ScIndexNodeCommon
  with IScStatUtil
  with AdnNodeMaybeAuth
{

  /** Базовая выдача для rcvr-узла sio-market. */
  def showcase(adnId: String, args: ScReqArgs) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    val _adnNodeFut = Future successful request.adnNode
    val helper = new ScIndexNodeSimpleHelper {
      override val geoListGoBackFut: Future[Option[Boolean]] = {
        for (mnode <- adnNodeFut) yield {
          mnode.geo.shapes
            .headOption.map(_.glevel.isLowest)
        }
      }
      override def _reqArgs = args
      override def adnNodeFut = _adnNodeFut
      override lazy val currAdnIdFut = Future successful Some(adnId)
      override def isGeo = false
      override implicit def _request = request
    }
    val resultFut = helper.result
    // собираем статистику, пока идёт подготовка результата
    val stat = scStatUtil.IndexStat(
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

