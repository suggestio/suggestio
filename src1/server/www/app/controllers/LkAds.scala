package controllers

import akka.stream.scaladsl.{Keep, Sink, Source}
import io.suggest.ads.{LkAdsFormConst, MLkAdsFormInit, MLkAdsOneAdResp}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.IMust
import io.suggest.init.routed.MJsiTgs
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.{MAdItemStatuses, MItems}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import javax.inject.Inject
import models.mctx.Context
import models.mproj.ICommonDi
import org.elasticsearch.search.sort.SortOrder
import play.api.http.HttpEntity
import play.api.libs.json.Json
import util.acl.IsNodeAdmin
import util.ad.JdAdUtil
import views.html.lk.ads._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 18:21
  * Description: Менеджер карточек в личном кабинете.
  * React-замена MarketLkAdn.showNodeAds() и связанным экшенам.
  */
class LkAds @Inject() (
                        isNodeAdmin             : IsNodeAdmin,
                        jdAdUtil                : JdAdUtil,
                        mNodes                  : MNodes,
                        streamsUtil             : StreamsUtil,
                        mItems                  : MItems,
                        override val mCommonDi  : ICommonDi
                      )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._
  import streamsUtil.Implicits._


  /** Рендер странице с react-формой управления карточками.
    * Сами карточки приходят отдельным экшеном, здесь же только страница с конфигом
    * формы карточек.
    *
    * @param nodeKey ключ узла - цепочка узлов.
    * @return 200 ОК + страница управления карточками узла.
    */
  def adsPage(nodeKey: RcvrKey) = csrf.AddToken {
    isNodeAdmin(nodeKey, U.Lk).async { implicit request =>
      // Заготовить контекст.
      val ctxFut = for {
        lkCtxData0 <- request.user.lkCtxDataFut
      } yield {
        implicit val lkCtxData2 = lkCtxData0.withJsiTgs(
          MJsiTgs.LkAdsForm :: lkCtxData0.jsiTgs
        )
        implicitly[Context]
      }

      // Собираем состояние формы:
      val initS = MLkAdsFormInit(
        nodeKey = nodeKey
      )
      val initJson = Json.toJson( initS )

      for {
        ctx <- ctxFut
      } yield {
        val html = lkAdsTpl(
          mnode   = request.mnode,
          state0  = initJson.toString
        )(ctx)

        Ok(html)
      }
    }
  }


  /** Запрос ещё карточек с сервера.
    * По сути, надо подготовить карточки, как для плитки.
    *
    * @param nodeKey Ключ до родительского узла.
    * @param offset1 Сдвиг в кол-ве карточек.
    * @return chunked JSON.
    */
  def getAds(nodeKey: RcvrKey, offset1: Int, newAdIdOpt: Option[String]) = isNodeAdmin(nodeKey).async { implicit request =>
    // TODO Добавить поддержку агрумента mode
    lazy val logPrefix = s"getAds(${nodeKey.mkString("/")}+$offset1${newAdIdOpt.fold("")("," + _)})#${System.currentTimeMillis()}:"

    // Нужно найти запрашиваемые карточки:
    val parentNodeId = nodeKey.last

    // Макс кол-во карточек за один запрос.
    val maxAdsPerTime = LkAdsFormConst.GET_ADS_COUNT_PER_REQUEST

    val adsSearch0 = new MNodeSearchDfltImpl {
      override val nodeTypes = MNodeTypes.Ad :: Nil
      override val outEdges  = {
        val must = IMust.MUST
        // Поиск по узлу-владельцу
        val crOwn = Criteria(
          predicates  = MPredicates.OwnedBy :: Nil,
          nodeIds     = parentNodeId :: Nil,
          must        = must
        )
        // Поиск только jd-карточкек
        val crJdAd = Criteria(
          predicates = MPredicates.JdContent :: Nil,
          must       = must
        )
        crOwn :: crJdAd :: Nil
      }
      override def limit     = maxAdsPerTime
      // TODO Почему-то сортировка работает задом наперёд, должно быть DESC тут:
      override val withDateCreatedSort = Some(SortOrder.ASC)
      override def offset    = offset1
    }

    // Быстро узнать id карточек, которые надо отобразить на экране:
    val madIdsFut = mNodes.dynSearchIds( adsSearch0 )

    // Возможна ситуация, что есть доп.карточка, которая не видна в поиске. Такое возможно,
    // если создать карточку в редакторе, сохранить её и сразу отредиректить юзера в список карточек.
    val createdAdOptFut = FutureUtil.optFut2futOpt(newAdIdOpt) { newAdId =>
      val headAdOrExFut = for {
        // Дождаться текущиго списка карточек:
        madIds <- madIdsFut
        // Узнать, есть ли карточка в этом списке?
        if !madIds.contains( newAdId )
        // Нет в списке, значит это свеже-созданная карточка, которую надо прочитать из БД и добавить в началов списка карточек:
        newAdOpt <- mNodesCache.getByIdType(newAdId, MNodeTypes.Ad)
        // Проверить права доступа на карточку: TODO Вынести проверку в ACL или дёргать уже существующую проверку.
        if newAdOpt.exists { newAd =>
          newAd.edges
            .withNodePred(parentNodeId, MPredicates.OwnedBy)
            .nonEmpty
        }
      } yield {
        LOGGER.trace(s"$logPrefix Accepted newAdId = $newAdId. Ad?${newAdOpt.nonEmpty}")
        newAdOpt
      }
      headAdOrExFut.recover { case _: NoSuchElementException =>
        LOGGER.warn(s"$logPrefix No or invalid prevAd, prevAdId=$newAdId")
        None
      }
    }

    // Поиск данных по размещениям в базе биллинга:
    val madId2advStatusesMapFut = madIdsFut.flatMap { madIds =>
      LOGGER.trace(s"$logPrefix madIds[${madIds.length}/$maxAdsPerTime] = ${madIds.mkString(", ")}")

      slick.db
        .stream(
          mItems.findStatusesForAds(
            // Не добавляем сюда newAdId, т.к. если у только что созданной карточки размещений быть и не должно.
            adIds    = madIds,
            statuses = MItemStatuses.advBusy.toStream
          )
        )
        .toSource
        .map { a =>
          a.nodeId -> a
        }
        .maybeTraceCount(this)(count => s"$logPrefix Bill agg $count item-infos for ${madIds.length} mads.")
        .toMat(
          Sink.collection[(String, MAdItemStatuses), Map[String, MAdItemStatuses]]
        )(Keep.right)
        .run()
    }

    implicit val ctx = getContext2

    // Собираем source из карточек, которые были найдены в поиске:
    val normalAdsSrc = Source
      .fromFuture(madIdsFut)
      .flatMapConcat( mNodes.multiGetSrc(_) )

    // Запихнуть свеже-созданную карточку newAdId в начало общего Source, если есть такая карточка.
    val allAdsSrc = newAdIdOpt.fold[Source[MNode, _]](normalAdsSrc) { _ =>
      Source.fromFutureSource {
        for (createdAdOpt <- createdAdOptFut) yield {
          LOGGER.trace(s"$logPrefix Newly created ad: ${createdAdOpt.flatMap(_.id)}")
          createdAdOpt.fold[Source[MNode, _]](normalAdsSrc) { newAd =>
            normalAdsSrc.prepend( Source.single(newAd) )
          }
        }
      }
    }

    // Рендерим карточки в потоке:
    val adsRenderedSrc = allAdsSrc
      // Параллельно рендерим запрошенные карточки:
      .mapAsync(8) { mad =>
        LOGGER.trace(s"$logPrefix Will render ${mad.idOrNull}")
        val mainTpl = jdAdUtil.getMainBlockTpl( mad )
        // Убрать wide-флаг в main strip'е, иначе будет плитка со строкой-дыркой.
        val mainNonWideTpl = jdAdUtil.setBlkWide(mainTpl, wide2 = false)
        val edges2 = jdAdUtil.filterEdgesForTpl(mainNonWideTpl, mad.edges)

        val jdAdDataFut = jdAdUtil.mkJdAdDataFor
          .show(
            nodeId        = mad.id,
            nodeEdges     = edges2,
            tpl           = mainNonWideTpl,
            // Тут по идее надо четверть или половину, но с учётом плотности пикселей можно округлить до 1.0. Это и нагрузку снизит.
            szMult        = 1.0f,
            allowWide     = false,
            forceAbsUrls  = false
          )(ctx)
          .execute()

        // Параллельно вычисляем некоторые другие параметры:
        // Видна ли карточка на родительском узле?
        val shownAtParent = mad.edges
          .withNodePred(parentNodeId, MPredicates.Receiver)
          .hasNext

        for {
          jdAdData <- jdAdDataFut
        } yield {
          LOGGER.trace(s"$logPrefix Done render ${mad.idOrNull}, ${jdAdData.edges.size} edges, rootJdt=${jdAdData.template.rootLabel}, shownAtParent?$shownAtParent")
          (mad.id, jdAdData, shownAtParent)
        }
      }

    // Поток отрендеренного JSON'а, который уже готов для отправки клиенту:
    val adsSrcJsonBytes = Source.fromFutureSource {
      for {
        madId2advStatusesMap <- madId2advStatusesMapFut
      } yield {
        adsRenderedSrc
          .map { case (madIdOpt, jdAdData, shownAtParent) =>
            val oneAdResp = MLkAdsOneAdResp(
              advStatuses = madIdOpt
                .flatMap( madId2advStatusesMap.get )
                .fold[Set[MItemStatus]](Set.empty)(_.statuses),
              jdAdData = jdAdData,
              shownAtParent = shownAtParent
            )
            Json.toJson( oneAdResp )
          }
          .jsValuesToJsonArrayByteStrings
      }
    }

    // Генерим HTTP-ответ.
    val rs = Ok

    // Сверяем версию HTTP, чтобы chunked тоже поддерживалась.
    val resp = if (request.version ==* HTTP_1_0) {
      LOGGER.debug(s"$logPrefix Non-http11 client: ${request.remoteClientAddress}. Can't chunk.")
      rs.sendEntity(
        HttpEntity.Streamed(
          data          = adsSrcJsonBytes,
          contentLength = None,
          contentType   = Some(JSON)
        )
      )
    } else {
      rs.chunked(adsSrcJsonBytes)
    }
    resp
      .as(JSON)
      .withHeaders(CACHE_CONTROL -> "public, max-age=10")
  }

}
