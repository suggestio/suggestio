package controllers.sc

import _root_.util.blocks.IBlkImgMakerDI
import _root_.util.showcase.{IScAdSearchUtilDi, IScUtil}
import _root_.util.stat.IStatUtil
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MSzMult
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.{IMNodes, MNode}
import io.suggest.primo.TypeT
import io.suggest.sc.MScApiVsns
import io.suggest.sc.ads.{MSc3AdData, MSc3AdsResp}
import io.suggest.sc.sc3.{MSc3RespAction, MScQs, MScRespActionTypes}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.IMacroLogs
import models.im.make.MakeResult
import models.req.IReq
import models.blk._
import util.acl._
import japgolly.univeq._

import scala.concurrent.Future
import models.blk
import util.ad.IJdAdUtilDi

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.11.14 16:47
 * Description: Поддержка плитки в контроллере: логика подготовки к сборке ответа.
 */
trait ScAdsTile
  extends ScController
  with IMacroLogs
  with IScUtil
  with IMNodes
  with IBlkImgMakerDI
  with IScAdSearchUtilDi
  with ICanEditAdDi
  with IStatUtil
  with IJdAdUtilDi
{

  import mCommonDi._

  // TODO Надо переписать рендер на reactive-streams: на больших нагрузка скачки расходования памяти и CPU могут стать нестепримыми.

  /** Изменябельная логика обработки запроса рекламных карточек для плитки. */
  trait TileAdsLogic extends LogicCommonT with IRespActionFut with TypeT {

    def _qs: MScQs

    /** 2014.11.25: Размер плиток в выдаче должен способствовать заполнению экрана по горизонтали,
      * избегая или минимизируя белые пустоты по краям экрана клиентского устройства. */
    lazy val tileArgs = scUtil.getTileArgs(_qs.common.screen)

    def szMult = tileArgs.szMult

    private def _brArgsFor(mad: MNode, bgImg: Option[MakeResult], indexOpt: Option[Int] = None): blk.RenderArgs = {
      blk.RenderArgs(
        mad           = mad,
        withEdit      = false,
        bgImg         = bgImg,
        szMult        = szMult,
        inlineStyles  = false,
        apiVsn        = _qs.common.apiVsn,
        indexOpt      = indexOpt,
        isFocused     = false
      )
    }

    def renderMadAsync(brArgs: blk.RenderArgs): Future[T]

    lazy val logPrefix = s"findAds(${ctx.timestamp}):"

    lazy val adSearch2Fut = scAdSearchUtil.qsArgs2nodeSearch( _qs )

    LOGGER.trace(s"$logPrefix ${_request.uri}")


    /** Найти все итоговые карточки. */
    lazy val madsFut: Future[Seq[MNode]] = {
      if (_qs.hasAnySearchCriterias) {
        for {
          adSearch <- adSearch2Fut
          res <- mNodes.dynSearch(adSearch)
        } yield {
          LOGGER.trace(s"$logPrefix Found ${res.size} ads")
          res
        }

      } else {
        // Нет поисковых критериев -- сразу же ничего не ищем.
        LOGGER.info(s"$logPrefix No data to ads search: ${_request.uri} remote ${_request.remoteClientAddress}")
        Future.successful(Nil)
      }
    }

    // Группировка groupNarrowAds отключена, т.к. новый focused-порядок не соответствует плитке,
    // а плитка страдает от выравнивания по 2 столбца.
    def madsGroupedFut = madsFut//.map { scUtil.groupNarrowAds }

    /** Очень параллельный рендер в HTML всех необходимых карточек. */
    def madsRenderedFut: Future[Seq[T]] = {
      // Запускаем асинхронные операции
      val _madsGroupedFut = madsGroupedFut
        .map { _.zipWithIndex }
      // Для доступа к offset для вычисления index (порядкового номера карточки).
      val offsetFut = adSearch2Fut
        .map { _.offset }

      // Получаем синхронные данные
      //val devScreenOpt = ctx.deviceScreenOpt
      //val _szMult = szMult

      // Продолжаем асинхронную обработку
      for {
        madsIndexed   <- _madsGroupedFut
        offset        <- offsetFut
        renderStartedAt = System.currentTimeMillis()
        madsRendered  <- {
          Future.traverse(madsIndexed) { case (mad, relIndex) =>
            val bgImgOptFut = Future.successful( Option.empty[MakeResult] )   // TODO mad2 BgImg.maybeMakeBgImgWith(mad, blkImgMaker, _szMult, devScreenOpt)
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
      val rcvrIdOpt = _qs.search.rcvrId
      val prodIdOpt = _qs.search.prodId

      val _rcvrOptFut   = mNodesCache.maybeGetByEsIdCached( rcvrIdOpt )
      val _prodOptFut   = mNodesCache.maybeGetByEsIdCached( prodIdOpt )

      val _userSaOptFut = statUtil.userSaOptFutFromRequest()
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
          statUtil.madsAction(_mads, MActionTypes.ScAdsTile),

          // Сохранить фактический search limit
          MAction(
            actions = MActionTypes.SearchLimit :: Nil,
            count   = _adSearch.limit :: Nil
          ),

          // Сохранить фактически search offset
          MAction(
            actions = MActionTypes.SearchOffset :: Nil,
            count   = _adSearch.offset :: Nil
          )
        )

        val saAcc = statUtil.withNodeAction(MActionTypes.ScRcvrAds, rcvrIdOpt, _rcvrOpt) {
          statUtil.withNodeAction( MActionTypes.ScProdAds, prodIdOpt, _prodOpt )(statActionsAcc0)
        }

        val generationOpt = _qs.search.genOpt

        new Stat2 {
          override def components = MComponents.Tile :: super.components
          override def statActions = saAcc
          override def userSaOpt = _userSaOpt
          override def locEnvOpt = _qs.common.locEnv.optional
          override def gen = generationOpt
          override def devScreenOpt = _qs.common.screen
        }
      }
    }

  }

  /** Компаньон логик для разруливания версий логик обработки HTTP-запросов. */
  protected object TileAdsLogic {

    /** Собрать необходимую логику обработки запроса в зависимости от версии API. */
    def apply(adSearch: MScQs)(implicit request: IReq[_]): TileAdsLogic = {
      val v = adSearch.common.apiVsn
      if (v.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
        new TileAdsLogicV3( adSearch )
      } else {
        throw new UnsupportedOperationException("Unsupported API version: " + v)
      }
    }

  }


  case class TileAdsLogicV3(override val _qs: MScQs)
                           (override implicit val _request: IReq[_]) extends TileAdsLogic {

    override type T = MSc3AdData

    // TODO brArgs содержит кучу неактуального мусора, потому что рендер уехал на клиент. Следует удалить лишние поля следом за v2-выдачей.
    override def renderMadAsync(brArgs: RenderArgs): Future[T] = {
      // Требуется рендер только main-блока карточки.
      Future {
        // Можно рендерить карточку сразу целиком, если на данном узле карточка размещена как заранее открытая.
        val isDisplayOpened = (_qs.search.rcvrId.toList reverse_::: _qs.search.tagNodeId.toList)
          .exists { nodeId =>
            brArgs.mad.edges
              .withPredicateIter( MPredicates.Receiver, MPredicates.TaggedBy )
              .exists { medge =>
                medge.nodeIds.contains(nodeId) &&
                  medge.info.flag.contains(true)
              }
          }

        // Узнать, какой шаблон рендерить.
        val tpl2 = if (isDisplayOpened) {
          LOGGER.trace(s"$logPrefix Ad#${brArgs.mad.idOrNull} renders focused by default.")
          jdAdUtil.getNodeTpl( brArgs.mad )
        } else {
          val tpl1 = jdAdUtil.getMainBlockTpl( brArgs.mad )
          // Убрать wide-флаг в main strip'е, иначе будет плитка со строкой-дыркой.
          jdAdUtil.setBlkWide(tpl1, wide2 = false)
        }

        // Собираем необходимые эджи и упаковываем в переносимый контейнер:
        val edges2 = jdAdUtil.filterEdgesForTpl(tpl2, brArgs.mad.edges)
        val jdFut = jdAdUtil.mkJdAdDataFor
          .show(
            nodeId        = brArgs.mad.id,
            nodeEdges     = edges2,
            tpl           = tpl2,
            szMult        = tileArgs.szMult,
            allowWide     = isDisplayOpened,
            forceAbsUrls  = _qs.common.apiVsn.forceAbsUrls
          )(ctx)
          .execute()

        val canEditFocusedOptFut = OptionUtil.maybeFut( isDisplayOpened ) {
          for (
            resOpt <- canEditAd.isUserCanEditAd(_request.user, brArgs.mad)
          ) yield {
            Some( resOpt.nonEmpty )
          }
        }

        for {
          jd <- jdFut
          canEditOpt <- canEditFocusedOptFut
        } yield {
          MSc3AdData(
            jd      = jd,
            canEdit = canEditOpt
          )
        }
      }
        .flatten
    }


    override def respActionFut: Future[MSc3RespAction] = {
      val _madsRenderFut = madsRenderedFut
      val szMult = MSzMult.fromDouble( tileArgs.szMult )

      // Завернуть index-экшен в стандартный scv3-контейнер:
      for {
        madsRendered <- _madsRenderFut
      } yield {
        MSc3RespAction(
          acType = MScRespActionTypes.AdsTile,
          ads = Some(MSc3AdsResp(
            ads     = madsRendered,
            szMult  = szMult
          ))
        )
      }
    }

  }

}
