package controllers.sc

import _root_.util.blocks.BgImg
import _root_.util.di.{IScNlUtil, IScStatUtil, IScUtil}
import _root_.util.blocks.IBlkImgMakerDI
import _root_.util.showcase.IScAdSearchUtilDi
import _root_.util.PlayMacroLogsI
import io.suggest.model.n2.node.IMNodes
import io.suggest.primo.TypeT
import io.suggest.stat.m.{MAction, MActionTypes}
import models.im.make.MakeResult
import models.msc._
import models.req.IReq
import play.api.mvc.Result
import models.blk._
import play.twirl.api.Html
import util.acl._
import play.api.libs.json._

import scala.collection.immutable
import scala.concurrent.Future
import models._
import models.msc.resp.{MScResp, MScRespAction, MScRespActionTypes, MScRespAdsTile}

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
  with IBlkImgMakerDI
  with IScAdSearchUtilDi
{

  import mCommonDi._

  /** Изменябельная логика обработки запроса рекламных карточек для плитки. */
  trait TileAdsLogic extends LogicCommonT with AdCssRenderArgs with TypeT {

    def _qs: MScAdsTileQs

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
        apiVsn        = _qs.apiVsn,
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

    lazy val logPrefix = s"findAds(${ctx.timestamp}):"

    lazy val adSearch2Fut = scAdSearchUtil.qsArgs2nodeSearch(_qs.search)

    /** Найти все итоговые карточки. */
    lazy val madsFut: Future[Seq[MNode]] = {
      if (_qs.search.hasAnySearchCriterias) {
        for {
          adSearch <- adSearch2Fut
          res <- mNodes.dynSearch(adSearch)
        } yield {
          LOGGER.trace(s"$logPrefix Found ${res.size} ads")
          res
        }

      } else {
        // Нет поисковых критериев -- сразу же ничего не ищем.
        LOGGER.info(s"$logPrefix No data to ads search: ${_request.uri} remote ${_request.remoteAddress}")
        Future.successful(Nil)
      }
    }

    /** Сборка аргументов рендера для пакетного рендера css-стилей. */
    lazy val madsBrArgs4CssFut: Future[Seq[blk.RenderArgs]] = {
      madsFut.flatMap { mads =>
        val _szMult = szMult
        val devScreenOpt = ctx.deviceScreenOpt
        Future.traverse(mads) { mad =>
          for {
            bgImgOpt <- BgImg.maybeMakeBgImgWith(mad, blkImgMaker, _szMult, devScreenOpt)
          } yield {
            _brArgsFor(mad, bgImgOpt)
          }
        }
      }
    }

    override def adsCssExternalFut: Future[Seq[AdCssArgs]] = {
      for (mads <- madsFut) yield {
        val _szMult = szMult
        mads
          .flatMap(_.id)
          .map { adId => AdCssArgs(adId, _szMult) }
      }
    }

    /** Параметры для рендера обрамляющего css блоков (css не полей, а блоков в целом). */
    override def adsCssRenderArgsFut: Future[immutable.Seq[IRenderArgs]] = {
      for {
        brArgss <- madsBrArgs4CssFut
      } yield {
        brArgss
          .toStream
      }
    }

    override def adsCssFieldRenderArgsFut: Future[immutable.Seq[FieldCssRenderArgsT]] = {
      for {
        brArgss <- madsBrArgs4CssFut
      } yield {
        brArgss
          .iterator
          .flatMap { brArgs =>  mad2craIter(brArgs, Nil) }
          .toStream
      }
    }


    // Группировка groupNarrowAds отключена, т.к. новый focused-порядок не соответствует плитке,
    // а плитка страдает от выравнивания по 2 столбца.
    def madsGroupedFut = madsFut//.map { scUtil.groupNarrowAds }

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
      for {
        madsIndexed   <- _madsGroupedFut
        offset        <- offsetFut
        renderStartedAt = System.currentTimeMillis()
        madsRendered  <- {
          Future.traverse(madsIndexed) { case (mad, relIndex) =>
            val bgImgOptFut = BgImg.maybeMakeBgImgWith(mad, blkImgMaker, _szMult, devScreenOpt)
            bgImgOptFut.flatMap { bgImgOpt =>
              val indexOpt = Some(offset + relIndex)
              val brArgs1 = _brArgsFor(mad, bgImgOpt, indexOpt)
              renderMadAsync(brArgs1)
            }
          }
        }
      } yield {
        LOGGER.trace(s"$logPrefix madsRenderedFut: Render took ${System.currentTimeMillis() - renderStartedAt} ms.")
        madsRendered
      }
    }

    /** Статистика этой вот плитки. */
    override def scStat: Future[Stat2] = {
      val _rcvrOptFut   = mNodeCache.maybeGetByEsIdCached( _qs.search.rcvrIdOpt )
      val _prodOptFut   = mNodeCache.maybeGetByEsIdCached( _qs.search.prodIdOpt )

      val _userSaOptFut = scStatUtil.userSaOptFutFromRequest()
      val _madsFut      = madsFut
      val _adSearchFut  = adSearch2Fut

      for {
        _userSaOpt  <- _userSaOptFut
        _rcvrOpt    <- _rcvrOptFut
        _prodOpt    <- _prodOptFut
        _mads       <- _madsFut
        _adSearch   <- _adSearchFut

      } yield {

        // Собираем stat-экшены с помощью аккумулятора...
        val statActionsAcc0 = List[MAction](

          // Подготовить данные статистики по отрендеренным карточкам:
          scStatUtil.madsAction(_mads, MActionTypes.ScAdsTile),

          // Сохранить фактический search limit
          MAction(
            actions = Seq( MActionTypes.SearchLimit ),
            count   = Seq( _adSearch.limit )
          ),

          // Сохранить фактически search offset
          MAction(
            actions = Seq( MActionTypes.SearchOffset ),
            count   = Seq( _adSearch.offset )
          )
        )

        val saAcc = scStatUtil.withNodeAction(MActionTypes.ScRcvrAds, _qs.search.rcvrIdOpt, _rcvrOpt) {
          scStatUtil.withNodeAction( MActionTypes.ScProdAds, _qs.search.prodIdOpt, _prodOpt )(statActionsAcc0)
        }

        new Stat2 {
          override def statActions  = saAcc
          override def userSaOpt    = _userSaOpt
          override def locEnvOpt    = _qs.search.locEnv.optional
          override def gen          = _qs.search.genOpt
          override def devScreenOpt = _qs.screen
        }
      }
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


  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина.
    * @param adSearch Поисковый запрос.
    * @return JSONP с рекламными карточками для рендера в выдаче.
    */
  def findAds(adSearch: MScAdsTileQs) = MaybeAuth().async { implicit request =>
    // В зависимости от версии API, используем ту или иную реализацию логики.
    val logic = TileAdsLogicV(adSearch)
    val resultFut = logic.resultFut

    // В фоне собираем статистику
    logic.saveScStat()

    // Возвращаем собираемый результат
    resultFut
  }


  /** Компаньон логик для разруливания версий логик обработки HTTP-запросов. */
  protected object TileAdsLogicV {
    /** Собрать необходимую логику обработки запроса в зависимости от версии API. */
    def apply(adSearch: MScAdsTileQs)(implicit request: IReq[_]): TileAdsLogicV = {
      adSearch.apiVsn match {
        case MScApiVsns.Sjs1 =>
          new TileAdsLogicV2(adSearch)
        case other =>
          throw new UnsupportedOperationException("Unsupported API version: " + other.versionNumber)
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


  /** Логика сборки HTTP-ответа для API v2. */
  protected class TileAdsLogicV2(override val _qs: MScAdsTileQs)
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
        // Собираем финальный ответ.
        val respData = MScResp(
          scActions = Seq(
            MScRespAction(
              acType = MScRespActionTypes.AdsTile,
              adsTile = Some(
                MScRespAdsTile(
                  mads    = _madsRender,
                  css     = Some(_css),
                  params  = Some(_params)
                )
              )
            )
          )
        )
        Ok( Json.toJson(respData) )
      }
    }
  }


}
