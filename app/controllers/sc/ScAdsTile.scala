package controllers.sc

import java.util.NoSuchElementException

import _root_.util.blocks.BgImg
import _root_.util.di.{IScNlUtil, IScStatUtil, IScUtil}
import _root_.util.jsa.{Js, JsAction, JsAppendById, SmRcvResp}
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.IMNodes
import models.im.make.{MakeResult, Makers}
import models.mctx.Context
import models.msc._
import models.req.IReq
import play.api.mvc.Result
import util.jsa.cbca.grid._
import models.blk._
import models.jsm.FindAdsResp
import play.twirl.api.Html
import util._
import util.acl._
import views.html.sc._
import play.api.libs.json._

import scala.collection.immutable
import scala.concurrent.Future
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.11.14 16:47
 * Description: Поддержка плитки в контроллере: логика подготовки к сборке ответа.
 */
trait ScAdsTileBase
  extends ScController
  with PlayMacroLogsI
  with IScNlUtil
  with IScUtil
  with ScCssUtil
  with IMNodes
{

  import mCommonDi._

  /** Изменябельная логика обработки запроса рекламных карточек для плитки. */
  trait TileAdsLogic extends AdCssRenderArgs {

    type T
    implicit def _request: IReq[_]
    def _adSearch: AdSearch

    lazy val ctx = implicitly[Context]

    /** 2014.11.25: Размер плиток в выдаче должен способствовать заполнению экрана по горизонтали,
      * избегая или минимизируя белые пустоты по краям экрана клиентского устройства. */
    lazy val tileArgs = scUtil.getTileArgs()(ctx)

    def szMult = tileArgs.szMult

    private def _brArgsFor(mad: MNode, bgImg: Option[MakeResult], indexOpt: Option[Int] = None): blk.RenderArgs = {
      blk.RenderArgs(
        mad           = mad,
        bc            = BlocksConf.applyOrDefault(mad),
        withEdit      = false,
        bgImg         = bgImg,
        szMult        = szMult,
        inlineStyles  = false,
        apiVsn        = _adSearch.apiVsn,
        indexOpt      = indexOpt,
        isFocused     = false
      )
    }
    
    def renderMad2html(brArgs: blk.RenderArgs): Html = {
      BlocksConf.DEFAULT
        .renderBlock(brArgs)(ctx)
    }

    def renderMad2htmlAsync(brArgs: blk.RenderArgs): Future[Html] = {
      Future {
        renderMad2html(brArgs)
      }
    }

    def renderMadAsync(brArgs: blk.RenderArgs): Future[T]

    lazy val logPrefix = s"findAds(${System.currentTimeMillis}):"
    lazy val gsiFut = _adSearch.geo.geoSearchInfoOpt

    lazy val adSearch2Fut: Future[AdSearch] = {
      if (_adSearch.outEdges.nonEmpty) {
        // В оригинале (pre-N2) тут добавлялся LVL_START_PAGE в список уровней.
        Future successful _adSearch

      } else if (_adSearch.geo.isWithGeo) {
        // При геопоиске надо найти узлы, географически подходящие под запрос. Затем, искать карточки по этим узлам.
        // TODO При таком поиске надо использовать cache-controle: private, если ip-геолокация.
        scNlUtil.detectCurrentNode(_adSearch.geo, _adSearch.geo.geoSearchInfoOpt)
          .map { gdr => Some(gdr.node) }
          .recover { case ex: NoSuchElementException => None }
          .map {
            case Some(adnNode) =>
              new AdSearchWrapper {
                override def _dsArgsUnderlying = _adSearch
                override def outEdges: Seq[ICriteria] = {
                  val p = MPredicates.Receiver
                  super.outEdges
                    .iterator
                    .filter { e => !e.containsPredicate(p) }
                    .++( Iterator(Criteria(adnNode.id.toSeq, Seq(p))) )
                    .toSeq
                }
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
        LOGGER.warn("Strange search request: " + _adSearch + " from " + _request.remoteAddress + " via " + _request.method + " " + _request.path)
        Future successful _adSearch
      }
    }

    lazy val madsFut: Future[Seq[MNode]] = {
      adSearch2Fut flatMap { adSearch2 =>
        mNodes.dynSearch(adSearch2)
      }
    }

    /** Сборка аргументов рендера для пакетного рендера css-стилей. */
    lazy val madsBrArgs4CssFut: Future[Seq[blk.RenderArgs]] = {
      madsFut flatMap { mads =>
        val _szMult = szMult
        val devScreenOpt = ctx.deviceScreenOpt
        Future.traverse(mads) { mad =>
          val bgImgOptFut = BgImg.maybeMakeBgImgWith(mad, Makers.Block, _szMult, devScreenOpt)
          bgImgOptFut map { bgImgOpt =>
            _brArgsFor(mad, bgImgOpt)
          }
        }
      }
    }

    override def adsCssExternalFut: Future[Seq[AdCssArgs]] = {
      madsFut map { mads =>
        val _szMult = szMult
        mads
          .flatMap(_.id)
          .map { adId => AdCssArgs(adId, _szMult) }
      }
    }

    /** Параметры для рендера обрамляющего css блоков (css не полей, а блоков в целом). */
    override def adsCssRenderArgsFut: Future[immutable.Seq[IRenderArgs]] = {
      madsBrArgs4CssFut map { brArgss =>
        brArgss
          .toStream
      }
    }

    override def adsCssFieldRenderArgsFut: Future[immutable.Seq[FieldCssRenderArgsT]] = {
      madsBrArgs4CssFut map { brArgss =>
        brArgss
          .iterator
          .flatMap { brArgs =>  mad2craIter(brArgs, Nil) }
          .toStream
      }
    }


    def madsGroupedFut = madsFut.map { scUtil.groupNarrowAds }

    /** Очень параллельный рендер в HTML всех необходимых карточек. */
    lazy val madsRenderedFut: Future[Seq[T]] = {
      // Запускаем асинхронные операции
      val _madsGroupedFut = madsGroupedFut
        .map { _.zipWithIndex }
      // Для доступа к offset для вычисления index (порядкового номера карточки).
      val offsetFut = adSearch2Fut
        .map { _.offset }

      // Получаем синхронные данные
      val devScreenOpt = ctx.deviceScreenOpt
      val _szMult = szMult

      // Продолжаем асинхронную обработку
      _madsGroupedFut flatMap { madsIndexed =>
        offsetFut flatMap { offset =>

          Future.traverse(madsIndexed) { case (mad, relIndex) =>
            val bgImgOptFut = BgImg.maybeMakeBgImgWith(mad, Makers.Block, _szMult, devScreenOpt)
            bgImgOptFut.flatMap { bgImgOpt =>
              val indexOpt = Some(offset + relIndex)
              val brArgs1 = _brArgsFor(mad, bgImgOpt, indexOpt)
              renderMadAsync(brArgs1)
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


/** Поддержка ответов на выдачу v1. */
trait ScAdsTile
  extends ScAdsTileBase
  with IScStatUtil
  with MaybeAuth
{

  import mCommonDi._

  /** Начальный размер буффера сборки ответа на запрос findAds(). */
  private val TILE_JS_RESP_BUFFER_SIZE_BYTES: Int = configuration.getInt("sc.tiles.jsresp.buffer.size.bytes") getOrElse 8192


  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина.
    * @param adSearch Поисковый запрос.
    * @return JSONP с рекламными карточками для рендера в выдаче.
    */
  def findAds(adSearch: AdSearch) = MaybeAuth().async { implicit request =>
    // В зависимости от версии API, используем ту или иную реализацию логики.
    val logic = TileAdsLogicV(adSearch)
    val resultFut = logic.resultFut

    // В фоне собираем статистику
    logic.madsFut onSuccess { case mads =>
      logic.adSearch2Fut onSuccess { case adSearch2 =>
        scStatUtil.TilesStat(adSearch2, mads.flatMap(_.id), logic.gsiFut)
          .saveStats
      }
    }

    // Возвращаем собираемый результат
    resultFut
  }


  /** Компаньон логик для разруливания версий логик обработки HTTP-запросов. */
  protected object TileAdsLogicV {
    /** Собрать необходимую логику обработки запроса в зависимости от версии API. */
    def apply(adSearch: AdSearch)(implicit request: IReq[_]): TileAdsLogicV = {
      adSearch.apiVsn match {
        case MScApiVsns.Coffee =>
          new TileAdsLogicV1(adSearch)
        case MScApiVsns.Sjs1 =>
          new TileAdsLogicV2(adSearch)
      }
    }
  }

  /** Action logic содержит в себе более конкретную логику для сборки http-json-ответа по findAds(). */
  protected trait TileAdsLogicV extends TileAdsLogic {
    /** Рендер HTTP-результата. */
    def resultFut: Future[Result]

    def cellSizeCssPx: Int    = szMulted(BlockWidths.NARROW.widthPx, tileArgs.szMult)
    def cellPaddingCssPx: Int = szMulted(scUtil.TILE_PADDING_CSSPX, tileArgs.szMult)
  }


  /** Логика сборки http-ответов для API v1. */
  protected class TileAdsLogicV1(val _adSearch: AdSearch)
                                (implicit val _request: IReq[_]) extends TileAdsLogicV {

    override type T = JsString

    /** v1 использует wrapper-шаблон с js-shop-link, не несущий никакой стилистики. */
    override def renderMad2html(brArgs: RenderArgs): Html = {
      _adNormalTpl(brArgs, isWithAction = true)(ctx)
    }

    override def renderMadAsync(brArgs: blk.RenderArgs): Future[T] = {
      for (html <- renderMad2htmlAsync(brArgs)) yield {
        htmlCompressUtil.html2jsStr(html)
      }
    }

    /** Сборка http-ответа для coffeescript-выдачи. Там использовался голый js. */
    override def resultFut: Future[Result] = {
      // Запускаем асинхронную сборку ответа.
      val smRcvRespFut = madsRenderedFut map { madsRendered =>
        val resp = FindAdsResp(madsRendered)
        SmRcvResp(resp)
      }
      // ссылку на css блоков надо составить и передать клиенту отдельно от тела основного ответа прямо в <head>.
      val cssAppendFut = jsAppendAdsCssFut
      // 2014.nov.25: Из-за добавления масштабирования блоков плитки нужно подкручивать на ходу значения в cbca_grid.
      val setCellSizeJsa = SetCellSize( cellSizeCssPx )
      val setCellPaddingJsa = SetCellPadding( cellPaddingCssPx )
      // resultFut содержит фьючерс с итоговым результатом работы экшена, который будет отправлен клиенту.
      for {
        smRcvResp <- smRcvRespFut
        cssAppend <- cssAppendFut
      } yield {
        cacheControlShort {
          Ok( Js(TILE_JS_RESP_BUFFER_SIZE_BYTES, setCellSizeJsa, setCellPaddingJsa, cssAppend, smRcvResp) )
        }
      }
    }

  }


  /** Логика сборки HTTP-ответа для API v2. */
  protected class TileAdsLogicV2(val _adSearch: AdSearch)
                                (implicit val _request: IReq[_]) extends TileAdsLogicV {

    override type T = MFoundAd

    override def renderMadAsync(brArgs: RenderArgs): Future[T] = {
      for ( html <- renderMad2htmlAsync(brArgs) ) yield {
        MFoundAd(
          htmlCompressUtil.html2str4json(html)
        )
      }
    }

    /** Рендер HTTP-результата. */
    override def resultFut: Future[Result] = {
      val _madsRenderFut = madsRenderedFut
      val _cssFut = jsAdsCssFut.map(_.body)
      val _params = MGridParams(
        cellSizeCssPx = cellSizeCssPx,
        cellPaddingCssPx = cellPaddingCssPx
      )
      for {
        _madsRender <- _madsRenderFut
        _css        <- _cssFut
      } yield {
        val respData = MFindAdsResp(
          mads    = _madsRender,
          css     = Some(_css),
          params  = Some(_params)
        )
        Ok( Json.toJson(respData) )
      }
    }
  }


}
