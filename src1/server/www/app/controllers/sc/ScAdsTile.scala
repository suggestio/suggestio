package controllers.sc

import _root_.util.blocks.IBlkImgMakerDI
import _root_.util.showcase.{IScAdSearchUtilDi, IScUtil, MRadioBeaconsSearchCtx}
import _root_.util.stat.IStatUtil
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.{MEsInnerHitsInfo, MEsNestedSearch, MEsUuId}
import io.suggest.es.search.MRandomSortData
import io.suggest.geo.MLocEnv
import io.suggest.jd.tags.JdTag
import io.suggest.n2.edge.{MEdgeFlags, MPredicate, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.extra.doc.MNodeDoc
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{IMNodes, MNode, MNodeFields, MNodeTypes}
import io.suggest.primo.TypeT
import io.suggest.sc.MScApiVsns
import io.suggest.sc.ads.{MAdsSearchReq, MSc3AdData, MSc3AdsResp, MScAdInfo, MScAdMatchInfo, MScNodeMatchInfo}
import io.suggest.sc.sc3.{MSc3RespAction, MScCommonQs, MScQs, MScRespActionTypes}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.IMacroLogs
import models.im.make.MakeResult
import models.req.IReq
import models.blk._
import util.acl._
import japgolly.univeq._

import scala.concurrent.Future
import models.blk
import scalaz.Tree
import util.ad.IJdAdUtilDi
import util.adn.INodesUtil

import scala.jdk.CollectionConverters._

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
  with INodesUtil
{

  import mCommonDi._
  import esModel.api._

  // TODO Надо переписать рендер на reactive-streams: на больших нагрузка скачки расходования памяти и CPU могут стать нестепримыми.

  /** Контейнер инфы по карточке. */
  protected case class MAdInfo(
                                mnode           : MNode,
                                matchInfos      : Iterable[MScAdMatchInfo] = Nil,
                                doc             : MNodeDoc,
                                mainBlock       : Tree[JdTag],
                                mainBlockIndex  : Int,
                              )

  /** Изменябельная логика обработки запроса рекламных карточек для плитки. */
  trait TileAdsLogic extends LogicCommonT with IRespActionFut with TypeT {

    def qsRaw: MScQs

    // TODO No lazy val here. Need "override val _qs = super._qs" on sub-classes.
    def _qs: MScQs = {
      val _qsRaw = qsRaw

      OptionUtil.maybeOpt( _qsRaw.grid.exists(_.onlyRadioBeacons) ) {
        // Grid patching for radio-beacons-only grid search. Remove all geoLoc and receiver data, if any:
        var modsAcc = List.empty[MScQs => MScQs]

        if (_qsRaw.common.locEnv.geoLocOpt.nonEmpty) {
          modsAcc ::= MScQs.common
            .composeLens( MScCommonQs.locEnv )
            .composeLens( MLocEnv.geoLocOpt )
            .set( None )
        }

        if (_qsRaw.search.rcvrId.nonEmpty) {
          modsAcc ::= MScQs.search
            .composeLens( MAdsSearchReq.rcvrId )
            .set( None )
        }

        modsAcc.reduceOption(_ andThen _)
      }
        .fold(_qsRaw)( _(_qsRaw) )
    }
    def radioSearchCtxFut: Future[MRadioBeaconsSearchCtx]

    private val scQs_common_locEnv_beacons_LENS =
      MScQs.common
        .composeLens( MScCommonQs.locEnv )
        .composeLens( MLocEnv.beacons )

    /** 2014.11.25: Размер плиток в выдаче должен способствовать заполнению экрана по горизонтали,
      * избегая или минимизируя белые пустоты по краям экрана клиентского устройства. */
    lazy val tileArgs = scUtil.getTileArgs(_qs.common.screen)

    def szMult = tileArgs.szMult

    private def _brArgsFor(mad: MNode, bgImg: Option[MakeResult], indexOpt: Option[Int] = None): blk.RenderArgs = {
      blk.RenderArgs(
        mad           = mad,
        withEdit      = false,
        bgImg         = bgImg,
        szMult        = szMult.toFloat,
        inlineStyles  = false,
        apiVsn        = _qs.common.apiVsn,
        indexOpt      = indexOpt,
        isFocused     = false
      )
    }

    def renderMadAsync(brArgs: blk.RenderArgs, adInfo: MAdInfo): Future[T]

    lazy val logPrefix = s"findAds(${ctx.timestamp}):"

    // Часть операций по проверке и чистке qs требуют асинхронных шагов. Делаем:
    lazy val _qsClearedFut = for {
      radioBeaconsCtx <- radioSearchCtxFut
    } yield {
      val beacons0 = scQs_common_locEnv_beacons_LENS.get( _qs )
      val qsBeacons2 = radioBeaconsCtx.qsBeacons2
      if (beacons0 !=* qsBeacons2) {
        (scQs_common_locEnv_beacons_LENS set qsBeacons2)( _qs )
      } else {
        _qs
      }
    }

    lazy val adSearch2Fut = for {
      radioSearchCtx  <- radioSearchCtxFut
      qsCleared       <- _qsClearedFut
    } yield {
      scAdSearchUtil.qsArgs2nodeSearch(
        args                  = qsCleared,
        subSearches           = radioSearchCtx.subSearches(
          innerHits = Some {
            MEsInnerHitsInfo(
              fields = (
                MNodeFields.Edges.EO_PREDICATE_FN ::
                  MNodeFields.Edges.EO_NODE_IDS_FN ::
                  Nil
                ),
            )
          },
          tagNodeId = qsCleared.search.tagNodeId,
        ),
      )
    }

    LOGGER.trace(s"$logPrefix ${_request.uri}")

    /** Найти все итоговые карточки. */
    lazy val madsFut: Future[Seq[MAdInfo]] = {
      if (_qs.hasAnySearchCriterias) {
        (for {
          qsCleared <- _qsClearedFut
          if qsCleared.hasAnySearchCriterias

          _adSearchFut = adSearch2Fut
          _radioSearchCtxFut = radioSearchCtxFut

          // Подготовить id для adn-узлов и id узлов-тегов.
          adnNodeIds = (qsCleared.search.rcvrId :: qsCleared.search.prodId :: Nil)
            .iterator
            .flatten
            .map(_.id)
            .toSet

          tagNodeIds = qsCleared.search.tagNodeId
            .iterator
            .map(_.id)
            .toSet

          adSearch <- _adSearchFut

          // Чтобы получить доступ к inner-hits-значениям, надо работать с исходным ES-ответом (SearchHit):
          // EsModel.source() тут использовать НЕЛЬЗЯ, т.к. сорсинг игнорирует критически важные limit/offset
          searchHits <- mNodes.search( adSearch )( mNodes.SearchHitsMapper )
          radioSearchCtx <- _radioSearchCtxFut

        } yield {
          val res = (for {
            searchHit <- searchHits.iterator

            // Распарсить узел-карточку:
            mNode = mNodes.deserializeOne2( searchHit )
            doc <- mNode.extras.doc
            (mainJdt, mainIndex) <- doc.template.getMainBlockOrFirst()

          } yield {
            // Надо получить инфу по найденным предикатам из inner_hits:
            val ihAdMatchInfos = (for {
              edgeInnerHits <- Option( searchHit.getInnerHits )
                .iterator
              // Process all innerHits without names, because many nested queries in OutEdges have different keys here.
              edgesIhHits <- edgeInnerHits
                .asScala
                .valuesIterator

              edgeIhHit <- edgesIhHits
                .getHits
                .iterator
              edgeIhHitFields = edgeIhHit.getFields

              // Поиск предиката в inner_hit:
              ihPreds = (for {
                ihPredField <- Option(
                  edgeIhHitFields.get( MNodeFields.Edges.EO_PREDICATE_FN )
                )
                  .iterator
                // Предикат у эджа один. Но тут возвращается цепочка id предикатов в неопределённом порядке.
                // В ES-5.x было: от parent к child. В любом случае, reduce до наиболее child-элемент.
                ihPredValues = ihPredField.getValues
                ihPredValRaw <- ihPredValues.iterator().asScala
                ihPredVal = ihPredValRaw.toString
                pred <- MPredicates.withValueOpt( ihPredVal )
              } yield {
                pred
              })
                .mostChildest
                .toSet

              // Поиск nodeIds в inner_hits.
              ihNodeIds = (for {
                ihNodeIdsField <- Option(
                  edgeIhHitFields.get( MNodeFields.Edges.EO_NODE_IDS_FN )
                )
                  .iterator
                ihNodeIdsValues = ihNodeIdsField.getValues
                ihNodeIdRaw <- ihNodeIdsValues.iterator().asScala
                ihNodeId = ihNodeIdRaw.toString
              } yield {
                ihNodeId
              })
                .toSet

              if ihPreds.nonEmpty || ihNodeIds.nonEmpty
            } yield {
              MScAdMatchInfo(
                predicates = ihPreds,
                nodeMatchings = ihNodeIds
                  .iterator
                  .map { nodeId =>
                    // Нужно найти связь с ble-маячком или иным узлом в контексте ble-search, если она есть.
                    // So, MNodeTypes.WifiAP type here is missing here (BleBeacon is used for Wifi). Possibly, these types must be mixed as RadioSignal-node subtypes.
                    val ntype = {
                      if ( radioSearchCtx.uidsClear contains nodeId ) Some( MNodeTypes.RadioSource.BleBeacon /* TODO Replace with MNodeTypes.RadioSource, when app v5.0.3+ will be installed (including GridAh & Sc3Circuit). */ )
                      else if (adnNodeIds contains nodeId) Some( MNodeTypes.AdnNode )
                      else if (tagNodeIds contains nodeId) Some( MNodeTypes.Tag )
                      else None
                    }
                    MScNodeMatchInfo(
                      nodeId = Some( nodeId ),
                      ntype  = ntype,
                    )
                  }
                  .toSeq
              )
            })
              .toSeq

            if (ihAdMatchInfos.nonEmpty)
              LOGGER.trace(s"$logPrefix Found ${ihAdMatchInfos.size} ad-matchings for nodeAd#${searchHit.getId}:\n ${ihAdMatchInfos.mkString(",\n ")}")

            MAdInfo( mNode, ihAdMatchInfos, doc, mainJdt, mainIndex )
          })
            // Сразу рендерим без лени.
            .to( List )

          LOGGER.trace(s"$logPrefix Found ${res.length} ads")
          res
        })
          .recover { case _: NoSuchElementException =>
            // Не осталось критериев для поиска карточек после очистки. Такое бывает, когда по результатам bluetooth-скана
            // появились маячки, незарегистрированные в suggest.io.
            LOGGER.trace(s"$logPrefix No search crs after async QS clearance. Was uncleared: ${_request.uri}")
            Nil
          }

      } else {
        // Нет поисковых критериев -- сразу же ничего не ищем.
        LOGGER.info(s"$logPrefix No data to ads search: ${_request.uri} remote ${_request.remoteClientAddress}")
        Future.successful(Nil)
      }
    }

    lazy val nodeId404 = nodesUtil.noAdsFound404RcvrId( ctx )

    /** Рекламные карточки, когда не найдено рекламных карточек. */
    def mads404Fut: Future[Seq[MNode]] = {
      val _nodeId404 = nodeId404
      LOGGER.trace(s"$logPrefix No ads found, will open from 404-node#${_nodeId404}")

      // Ищем карточки в узле-404 и их возвращаем:
      val msearchAds404 = new MNodeSearch {
        override val nodeTypes = MNodeTypes.Ad :: Nil
        override val outEdges: MEsNestedSearch[Criteria] = {
          val cr = Criteria(
            nodeIds = _nodeId404 :: Nil,
            predicates = MPredicates.Receiver :: Nil
          )
          MEsNestedSearch.plain( cr )
        }
        override val randomSort: Option[MRandomSortData] = {
          for (gen <- _qs.search.genOpt) yield
            MRandomSortData(gen)
        }
        override def offset = 0
        override def limit = nodesUtil.MAX_404_ADS_ONCE
      }
      val ads404Fut = mNodes.dynSearch(msearchAds404)

      if (LOGGER.underlying.isWarnEnabled)
        for (ads404 <- ads404Fut) {
          if (ads404.isEmpty)
            LOGGER.warn(s"$logPrefix NO 404-ADS found for 404-node#$nodeId404")
          else
            LOGGER.trace(s"$logPrefix Returning ${ads404.length} of 404-ads: [${ads404.iterator.flatMap(_.id).mkString(", ")}]")
        }

      ads404Fut
    }


    /** Очень параллельный рендер в HTML всех необходимых карточек. */
    def madsRenderedFut: Future[Seq[T]] = {
      // Для доступа к offset для вычисления index (порядкового номера карточки).
      val offsetFut = adSearch2Fut
        .map { _.offset }

      val _madsFut = for {
        mads    <- madsFut
        offset  <- offsetFut
        mads2   <- {
          // Если на первом ads-запросе не найдено карточек, и это НЕ скан маячков, то вернуть 404-карточки (когда они разрешаются клиентом).
          if (mads.isEmpty && _qs.grid.allow404 && offset <= 0) {
            // TODO Передавать на клиент, что нет больше карточек, чтобы не было дальнейшего запроса подгрузки ещё карточек.
            LOGGER.trace(s"$logPrefix mads[${mads.length}] offset=$offset => 404")
            for (mads404 <- mads404Fut) yield {
              for {
                mad404 <- mads404
                doc <- mad404.extras.doc
                (mainJdt, mainIndex) <- doc.template.getMainBlockOrFirst()
              } yield {
                MAdInfo(
                  mnode = mad404,
                  // Сообщить на клиент, что это 404-карточка, чтобы клиент НЕ уведомлял юзера о какой-либо полезной инфе.
                  matchInfos = MScAdMatchInfo(
                    predicates = Set.empty[MPredicate] + MPredicates.Receiver,
                    nodeMatchings = MScNodeMatchInfo(
                      nodeId = Some( nodeId404 ),
                    ) :: Nil
                  ) :: Nil,
                  mainBlock = mainJdt,
                  mainBlockIndex = mainIndex,
                  doc = doc,
                )
              }
            }
          } else {
            Future.successful(mads)
          }
        }
      } yield {
        if (mads2.isEmpty)
          LOGGER.debug(s"$logPrefix Missing all/any ads, NO 404 ads from [$nodeId404], something going wrong.")
        mads2.zipWithIndex
      }

      // Продолжаем асинхронную обработку
      for {
        madInfos      <- _madsFut
        offset        <- offsetFut
        renderStartedAt = System.currentTimeMillis()
        madsRendered  <- {
          Future.traverse(madInfos) { case (adInfo, relIndex) =>
            val indexOpt = Some(offset + relIndex)
            val brArgs1 = _brArgsFor(
              mad       = adInfo.mnode,
              bgImg     = None,
              indexOpt  = indexOpt,
            )
            renderMadAsync( brArgs1, adInfo )
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

      val _rcvrOptFut   = mNodes.maybeGetByEsIdCached( rcvrIdOpt )
      val _prodOptFut   = mNodes.maybeGetByEsIdCached( prodIdOpt )

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
          statUtil.madsAction(_mads.map(_.mnode), MActionTypes.ScAdsTile),

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
  object TileAdsLogic {

    /** Собрать необходимую логику обработки запроса в зависимости от версии API. */
    def apply(adSearch: MScQs, radioSearchCtxFut: Future[MRadioBeaconsSearchCtx])(implicit request: IReq[_]): TileAdsLogic = {
      val v = adSearch.common.apiVsn
      if (v.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
        new TileAdsLogicV3( adSearch, radioSearchCtxFut )
      } else {
        throw new UnsupportedOperationException("Unsupported API version: " + v)
      }
    }

  }


  case class TileAdsLogicV3(override val qsRaw: MScQs,
                            override val radioSearchCtxFut: Future[MRadioBeaconsSearchCtx],
                           )(override implicit val _request: IReq[_]) extends TileAdsLogic {

    override type T = MSc3AdData

    override val _qs = super._qs

    /** Список ресиверов, в которых допускается рендер карточек в-раскрытую. */
    private lazy val _adDisplayOpenedRcvrIds = {
      MEsUuId(nodeId404) ::
        _qs.search.rcvrId.toList reverse_:::
        _qs.search.tagNodeId.toList
    }

    // TODO brArgs содержит кучу неактуального мусора, потому что рендер уехал на клиент. Следует удалить лишние поля следом за v2-выдачей.
    override def renderMadAsync(brArgs: RenderArgs, adInfo: MAdInfo): Future[T] = {
      // Требуется рендер только main-блока карточки.
      Future {
        // Можно рендерить карточку сразу целиком, если на данном узле карточка размещена как заранее открытая.
        // 404-узел сюда же на правах костыля:
        val isDisplayOpened = _adDisplayOpenedRcvrIds
          .exists { nodeId =>
            brArgs.mad.edges
              .withPredicateIter( MPredicates.Receiver, MPredicates.TaggedBy )
              .exists { medge =>
                (medge.nodeIds contains nodeId.id) &&
                (medge.info.flagsMap contains MEdgeFlags.AlwaysOpened)
              }
          }

        // Узнать, какой шаблон рендерить.
        val (tpl2, selPathRev): (Tree[JdTag], List[Int]) = if (isDisplayOpened) {
          LOGGER.trace(s"$logPrefix Ad#${brArgs.mad.idOrNull} renders focused by default.")
          adInfo.doc.template -> Nil
        } else {
          // Убрать wide-флаг в main strip'е, иначе будет плитка со строкой-дыркой.
          jdAdUtil.resetBlkWide( adInfo.mainBlock ) -> (adInfo.mainBlockIndex :: Nil)
        }

        // Собираем необходимые эджи и упаковываем в переносимый контейнер:
        val edges2 = jdAdUtil.filterEdgesForTpl(tpl2, brArgs.mad.edges)
        val jdFut = jdAdUtil.mkJdAdDataFor
          .show(
            nodeId        = brArgs.mad.id,
            nodeEdges     = edges2,
            tpl           = tpl2,
            jdConf        = tileArgs,
            allowWide     = isDisplayOpened,
            selPathRev    = selPathRev,
            // Рендерить заголовки карточек в ответе только когда запрошено с клиента. А запрашивается это обычно для нотификаций.
            nodeTitle     = OptionUtil.maybeOpt( _qs.grid.exists(_.withTitle) )(
              adInfo.mnode.meta.basic.nameOpt
            ),
            scApiVsn      = Some( _qs.common.apiVsn ),
          )(ctx)
          .execute()

        val canEditFocusedOptFut = OptionUtil.maybeFut( isDisplayOpened ) {
          for {
            resOpt <- canEditAd.isUserCanEditAd(_request.user, brArgs.mad)
          } yield {
            Some( resOpt.nonEmpty )
          }
        }

        // Сборка прототипа JSON, описывающего один результат по карточке.
        for {
          jd <- jdFut
          canEditOpt <- canEditFocusedOptFut
        } yield {
          MSc3AdData(
            jd   = jd,
            info = MScAdInfo(
              canEditOpt      = canEditOpt,
              flags           = scUtil.collectScRcvrFlags( _qs, brArgs.mad ),
              matchInfos      = adInfo.matchInfos,
            )
          )
        }
      }
        .flatten
    }


    override def respActionFut: Future[MSc3RespAction] = {
      val _madsRenderFut = madsRenderedFut

      // Завернуть index-экшен в стандартный scv3-контейнер:
      for {
        madsRendered <- _madsRenderFut
      } yield {
        MSc3RespAction(
          acType = MScRespActionTypes.AdsTile,
          ads = Some(MSc3AdsResp(
            ads     = madsRendered,
            szMult  = tileArgs.szMult
          ))
        )
      }
    }

  }

}
