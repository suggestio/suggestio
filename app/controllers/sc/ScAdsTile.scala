package controllers.sc

import java.util.NoSuchElementException

import _root_.util.blocks.BgImg
import _root_.util.jsa.{JsAppendById, JsAction, SmRcvResp, Js}
import models.im.make.Makers
import util.jsa.cbca.grid._
import _root_.util.showcase._
import ShowcaseUtil._
import io.suggest.ym.model.common.SlNameTokenStr
import models.blk.{CssRenderArgsT, BlockWidths, FieldCssRenderArgsT}
import models.jsm.{FindAdsResp, SearchAdsResp}
import play.twirl.api.Html
import util._
import util.acl._
import views.html.sc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.{current, configuration}
import SiowebEsUtil.client
import scala.collection.immutable
import scala.concurrent.Future
import models._
import models.blk.szMulted

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.11.14 16:47
 * Description: Поддержка плитки в контроллере: экшен и прочая логика.
 */
trait ScAdsTile extends ScController with PlayMacroLogsI {

  /** Начальный размер буффера сборки ответа на запрос findAds(). */
  private val TILE_JS_RESP_BUFFER_SIZE_BYTES: Int = configuration.getInt("sc.tiles.jsresp.buffer.size.bytes") getOrElse 8192

  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина.
    * @param adSearch Поисковый запрос.
    * @return JSONP с рекламными карточками для рендера в выдаче.
    */
  def findAds(adSearch: AdSearch) = MaybeAuth.async { implicit request =>
    val logic = new TileAdsLogic {
      override type T = JsString
      override implicit def _request = request
      override def _adSearch: AdSearch = adSearch
      override def renderMadAsync(mad: MAd, brArgs: blk.RenderArgs): Future[T] = {
        renderMad2html(mad, brArgs)
          .map { html2jsStr }
      }
    }
    // Запускаем асинхронную сборку ответа.
    val smRcvRespFut = logic.madsRenderedFut map { madsRendered =>
      val resp = if (logic.catsRequested) {
        SearchAdsResp(madsRendered)
      } else {
        FindAdsResp(madsRendered)
      }
      SmRcvResp(resp)
    }
    // ссылку на css блоков надо составить и передать клиенту отдельно от тела основного ответа прямо в <head>.
    val cssAppendFut = logic.jsAppendAdsCss
    val tileArgs = logic.tileArgs
    // 2014.nov.25: Из-за добавления масштабирования блоков плитки нужно подкручивать на ходу значения в cbca_grid.
    val setCellSizeJsa = SetCellSize( szMulted(BlockWidths.NARROW.widthPx, tileArgs.szMult) )
    val setCellPaddingJsa = SetCellPadding( szMulted(ShowcaseUtil.TILE_PADDING_CSSPX, tileArgs.szMult) )
    // resultFut содержит фьючерс с итоговым результатом работы экшена, который будет отправлен клиенту.
    val resultFut = for {
      smRcvResp <- smRcvRespFut
      cssAppend <- cssAppendFut
    } yield {
      cacheControlShort {
        Ok( Js(TILE_JS_RESP_BUFFER_SIZE_BYTES, setCellSizeJsa, setCellPaddingJsa, cssAppend, smRcvResp) )
      }
    }
    // В фоне собираем статистику
    logic.madsFut onSuccess { case mads =>
      logic.adSearch2Fut onSuccess { case adSearch2 =>
        ScTilesStatUtil(adSearch2, mads.flatMap(_.id), logic.gsiFut)
          .saveStats
      }
    }
    // Возвращаем собираемый результат
    resultFut
  }


  /** Логика экшена, занимающегося обработкой запроса тут. */
  trait TileAdsLogic extends AdCssRenderArgs {
    
    type T
    implicit def _request: AbstractRequestWithPwOpt[_]
    def _adSearch: AdSearch

    lazy val ctx = implicitly[Context]

    /** 2014.11.25: Размер плиток в выдаче должен способствовать заполнению экрана по горизонтали,
      * избегая или минимизируя белые пустоты по краям экрана клиентского устройства. */
    lazy val tileArgs = ShowcaseUtil.getTileArgs()(ctx)

    /** Общие параметры для рендера всех блоков плитки. Они могут/будут перезаписаны позднее. */
    lazy val brArgsStubFut: Future[blk.RenderArgs] = {
      val ra = blk.RenderArgs(
        withEdit      = false,
        bgImg         = None,
        szMult        = tileArgs.szMult,
        inlineStyles  = false
      )
      Future successful ra
    }

    def renderMad2html(mad: MAd, brArgs: blk.RenderArgs): Future[Html] = {
      Future {
        _adNormalTpl(mad, args = brArgs, isWithAction = true)(ctx)
      }
    }

    def renderMadAsync(mad: MAd, brArgs: blk.RenderArgs): Future[T]

    lazy val logPrefix = s"findAds(${System.currentTimeMillis}):"
    lazy val gsiFut = _adSearch.geo.geoSearchInfoOpt

    def catsRequested = _adSearch.catIds.nonEmpty

    lazy val adSearch2Fut: Future[AdSearch] = {
      if (catsRequested) {
        Future.successful(_adSearch)
      } else {
        // При поиске по категориям надо искать только если есть указанный show level.
        if (_adSearch.catIds.nonEmpty) {
          val result = new AdSearchWrapper {
            override def _dsArgsUnderlying = _adSearch
            override def levels: Seq[SlNameTokenStr] = AdShowLevels.LVL_CATS :: super.levels.toList
          }
          Future successful result
        } else if (_adSearch.receiverIds.nonEmpty) {
          // TODO Можно спилить этот костыль?
          val result = new AdSearchWrapper {
            override def _dsArgsUnderlying = _adSearch
            override val levels: Seq[SlNameTokenStr] = (AdShowLevels.LVL_START_PAGE :: _adSearch.levels.toList).distinct
          }
          Future successful result
        } else if (_adSearch.geo.isWithGeo) {
          // TODO При таком поиске надо использовать cache-controle: private, если ip-геолокация.
          // При геопоиске надо найти узлы, географически подходящие под запрос. Затем, искать карточки по этим узлам.
          ShowcaseNodeListUtil.detectCurrentNode(_adSearch.geo, _adSearch.geo.geoSearchInfoOpt)
            .map { gdr => Some(gdr.node) }
            .recover { case ex: NoSuchElementException => None }
            .map {
              case Some(adnNode) =>
                new AdSearchWrapper {
                  override def _dsArgsUnderlying = _adSearch
                  override def receiverIds = List(adnNode.id.get)
                  override def geo: GeoMode = GeoNone
                }
              case None =>
                _adSearch
            }
            .recover {
              // Допустима работа без геолокации при возникновении внутренней ошибки.
              case ex: Throwable =>
                LOGGER.error(logPrefix + " Failed to get geoip info for " + _request.remoteAddress, ex)
                _adSearch
            }
        } else {
          // Слегка неожиданные параметры запроса.
          LOGGER.warn(logPrefix + " Strange search request: " + _adSearch)
          Future successful _adSearch
        }
      }
    }

    lazy val madsFut: Future[Seq[MAd]] = adSearch2Fut flatMap { adSearch2 =>
      MAd.dynSearch(adSearch2)
    }

    /** Связка из mads+brArgs используется в нескольких местах.
      * Тут фьючерс для упрощения и ускорения доступа к этой связке. */
    private lazy val madsAndBrArgs: Future[(Seq[MAd], blk.RenderArgs)] = {
      val _madsFut = madsFut
      for {
        brArgs <- brArgsStubFut
        mads   <- _madsFut
      } yield {
        (mads, brArgs)
      }
    }

    override def adsCssExternalFut: Future[Seq[AdCssArgs]] = {
      madsAndBrArgs map { case (mads, brArgs) =>
        val szMult = brArgs.szMult
        mads
          .flatMap(_.id)
          .map { adId => AdCssArgs(adId, szMult) }
      }
    }

    /** Параметры для рендера обрамляющего css блоков (css не полей, а блоков в целом). */
    override def adsCssRenderArgsFut: Future[immutable.Seq[CssRenderArgsT]] = {
      madsAndBrArgs map { case (mads, brArgs) =>
        mads
          .iterator
          .map { mad =>  blk.CssRenderArgs(mad, brArgs) }
          .toStream
      }
    }

    override def adsCssFieldRenderArgsFut: Future[immutable.Seq[FieldCssRenderArgsT]] = {
      madsAndBrArgs map { case (mads, brArgs) =>
        mads
          .iterator
          .flatMap { mad =>  mad2craIter(mad, brArgs, Nil) }
          .toStream
      }
    }


    lazy val madsGroupedFut = madsFut.map { groupNarrowAds }

    /** Очень параллельный рендер в HTML всех необходимых карточек. */
    lazy val madsRenderedFut: Future[Seq[T]] = {
      val _madsGroupedFut = madsGroupedFut
      brArgsStubFut flatMap { brArgs0 =>
        val devScreenOpt = ctx.deviceScreenOpt
        _madsGroupedFut flatMap { mads1 =>
          parTraverseOrdered(mads1) { (mad, index) =>
            BgImg.maybeMakeBgImgWith(mad, Makers.Block, brArgs0.szMult, devScreenOpt)
              .flatMap { bgImgOpt =>
                val brArgs1 = brArgs0.copy(
                  bgImg = bgImgOpt
                )
                renderMadAsync(mad, brArgs1)
              }
          }
        }
      }
    }
    
    override def jsAppendCssAction(html: JsString): JsAction = {
      JsAppendById("smResources", html)
    }
  }

}
