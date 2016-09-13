package controllers.sc

import _root_.util.di._
import _root_.util.PlayMacroLogsI
import models.im.MImgT
import models.jsm.ScIndexResp
import models.mctx.Context
import models.mproj.IMCommonDi
import models.msc._
import models.req.IReq
import play.twirl.api.Html
import util.acl._
import views.html.sc._
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
trait ScIndexConstants extends IMCommonDi {

  import mCommonDi.configuration

  /** Кеш ответа showcase(adnId) на клиенте. */
  val SC_INDEX_CACHE_SECONDS: Int = configuration.getInt("market.showcase.index.node.cache.client.seconds").getOrElse(20)

}



/** Общая утиль, испрользуемая в контроллере. */
trait ScIndexCommon
  extends ScController
  with PlayMacroLogsI
  with IStatUtil
{

  import mCommonDi._

  /** Базовый трейт для написания генератора производных indexTpl и ответов. */
  trait ScIndexHelperBase {
    def renderArgsFut: Future[ScRenderArgs]
    def currAdnIdFut: Future[Option[String]]
    def _reqArgs: ScReqArgs

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

    implicit def _request: IReq[_]

    /** Контейнер палитры выдачи. */
    def colorsFut: Future[IColors]

    implicit lazy val ctx: Context = getContext2

    def respHtmlFut: Future[Html] = {
      for (renderArgs <- renderArgsFut) yield {
        indexTpl(renderArgs)(ctx)
      }
    }
    def respHtmlJsFut = respHtmlFut.map( htmlCompressUtil.html2jsStr )


    def hBtnArgsFut: Future[HBtnArgs] = {
      for (colors <- colorsFut) yield {
        HBtnArgs(fgColor = colors.fgColor)
      }
    }

    /** Кнопка навигации, которая будет отрендерена в левом верхнем углу indexTpl. */
    def topLeftBtnHtmlFut: Future[Html] = {
      for (_hBtnArgs <- hBtnArgsFut) yield {
        val rargs = new ScReqArgsWrapper with IHBtnArgsFieldImpl {
          override def reqArgsUnderlying = _reqArgs
          override def hBtnArgs = _hBtnArgs
        }
        hdr._navPanelBtnTpl(rargs)(ctx)
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
          currAdnId       = currAdnIdOpt,
          geoAccurEnought = geoAccurEnought,
          titleOpt        = titleOpt
        )
      }
    }

    protected def _resultVsn(args: ScIndexResp): Future[Result] = {
      _reqArgs.apiVsn match {
        case MScApiVsns.Sjs1 =>
          Ok(args.toJson)
        case other =>
          throw new UnsupportedOperationException("Unsupported API vsn: " + other.versionNumber)
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
{

  import mCommonDi._

  /** Логика формирования indexTpl для конкретного узла. */
  trait ScIndexNodeHelper extends ScIndexHelperBase {
    def adnNodeFut        : Future[MNode]
    def geoListGoBackFut  : Future[Option[Boolean]]
    override lazy val currAdnIdFut: Future[Option[String]] = adnNodeFut.map(_.id)

    /** В рамках showcase(adnId) геолокация не требуется, узел и так известен. */
    override def geoAcurrEnoughtFut: Future[Option[Boolean]] = {
      Future.successful( None )
    }

    /** Есть узел -- есть заголовок. */
    override def titleOptFut: Future[Option[String]] = {
      adnNodeFut
        .map { _node2titleOpt }
    }

    override lazy val hBtnArgsFut = super.hBtnArgsFut

    /** Если узел с географией не связан, и есть "предыдущий" узел, то надо отрендерить кнопку "назад". */
    override def topLeftBtnHtmlFut: Future[Html] = {
      // Сразу запускаем сборку аргументов hbtn-рендера. Не здесь, так в super-методе они понадобятся точно.
      val _hBtnArgsFut = hBtnArgsFut

      // В методе логика немного разветвляется и асинхронна внутри. false-ветвь реализована через Future.failed.
      val fut0 = if (_reqArgs.prevAdnId.nonEmpty) {
        Future.successful( None )
      } else {
        Future.failed( new NoSuchElementException() )
      }

      // Отрендерить кнопку "назад на предыдущий узел", если всё ок...
      val htmlFut = for {
        _     <- fut0
        mnode <- adnNodeFut
        // Продолжать только если текущий узел не связан с географией.
        if {
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
        }
        // Наконец, обратиться к аргументам рендера кнопки.
        hBtnArgs0 <- _hBtnArgsFut
      } yield {
        // Отрендерить кнопку, внеся кое-какие коррективы в аргументы рендера.
        val hBtnArgs2 = hBtnArgs0.copy(adnId = _reqArgs.prevAdnId)
        ScHdrBtns.Back2UpperNode(hBtnArgs2)
      }

      // Что-то не так, но обычно это нормально.
      htmlFut.recoverWith { case ex: Throwable =>
        if (!ex.isInstanceOf[NoSuchElementException])
          LOGGER.error("topLeftBtnHtmlFut(): Workarounding unexpected expection", ex)
        super.topLeftBtnHtmlFut
      }
    }

    /** Получение карточки приветствия. */
    def welcomeAdOptFut: Future[Option[WelcomeRenderArgsT]] = {
      if (_reqArgs.withWelcomeAd) {
        adnNodeFut.flatMap { adnNode =>
          welcomeUtil.getWelcomeRenderArgs(adnNode, ctx.deviceScreenOpt)(ctx)
        }
      } else {
        Future.successful(None)
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
      for (adnNode <- adnNodeFut) yield {
        val _bgColor = adnNode.meta.colors.bg.fold(scUtil.SITE_BGCOLOR_DFLT)(_.code)
        val _fgColor = adnNode.meta.colors.fg.fold(scUtil.SITE_FGCOLOR_DFLT)(_.code)
        Colors(bgColor = _bgColor, fgColor = _fgColor)
      }
    }

    /** Приготовить аргументы рендера выдачи. */
    override def renderArgsFut: Future[ScRenderArgs] = {
      val _geoListGoBackFut   = geoListGoBackFut
      val _adnNodeFut         = adnNodeFut
      val _logoImgOptFut      = logoImgOptFut
      val _colorsFut          = colorsFut
      val _hBtnArgsFut        = hBtnArgsFut
      val _topLeftBtnHtmlFut  = topLeftBtnHtmlFut
      for {
        waOpt           <- welcomeAdOptFut
        adnNode         <- _adnNodeFut
        _geoListGoBack  <- _geoListGoBackFut
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
          override def logoImgOpt         = _logoImgOpt
          override def geoListGoBack      = _geoListGoBack
          override def welcomeOpt         = waOpt
        }
      }
    }

  }

}


/** Экшены для рендера indexTpl нод. */
trait ScIndexNode
  extends ScIndexNodeCommon
  with IScStatUtil
  with AdnNodeMaybeAuth
{

  import mCommonDi._

  /** Базовая выдача для rcvr-узла sio-market. */
  def showcase(adnId: String, args: ScReqArgs) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    val _adnNodeFut = Future.successful( request.mnode )

    val helper = new ScIndexNodeHelper {
      override def geoListGoBackFut: Future[Option[Boolean]] = {
        for (mnode <- adnNodeFut) yield {
          // TODO Что за задумка у этого кода, понять так и не получилось. Гео-шейп наугад имеет isLowest -> значит нужно Some(true).
          mnode.edges
            .withPredicateIter( MPredicates.NodeLocation )
            .flatMap(_.info.geoShapes)
            .toSeq.headOption
            .map(_.glevel.isLowest)
        }
      }
      override def _reqArgs = args
      override def adnNodeFut = _adnNodeFut
      override lazy val currAdnIdFut = Future.successful( Some(adnId) )
      override implicit def _request = request
    }

    val resultFut = helper.result

    // собираем статистику, пока идёт подготовка результата
    val stat = scStatUtil.IndexStat(
      gsiFut    = args.geo.geoSearchInfoOpt,
      screenOpt = helper.ctx.deviceScreenOpt,
      nodeOpt   = Some(request.mnode)
    )
    stat.saveStats.onFailure { case ex =>
      LOGGER.warn(s"showcase($adnId): failed to save stats, args = $args", ex)
    }

    // Возвращаем результат основного действа. Результат вполне кешируем по идее.
    for (res <- resultFut) yield {
      res.withHeaders(CACHE_CONTROL -> s"public, max-age=$SC_INDEX_CACHE_SECONDS")
    }
  }

}
