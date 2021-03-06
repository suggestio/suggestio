package controllers

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import io.suggest.ads.{LkAdsFormConst, MLkAdsFormInit, MLkAdsOneAdResp}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.fut.FutureUtil
import io.suggest.ctx.CtxData
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.init.routed.MJsInitTargets
import io.suggest.jd.MJdConf
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.{MAdItemStatuses, MItems}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.sec.util.Csrf
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._

import javax.inject.Inject
import models.mctx.Context
import org.elasticsearch.search.sort.SortOrder
import play.api.http.HttpEntity
import play.api.libs.json.Json
import util.acl.{IsNodeAdmin, SioControllerApi}
import util.ad.JdAdUtil
import views.html.lk.ads._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 18:21
  * Description: Менеджер карточек в личном кабинете.
  * React-замена MarketLkAdn.showNodeAds() и связанным экшенам.
  */
final class LkAds @Inject() (
                              sioControllerApi        : SioControllerApi,
                            )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import slickHolder.slick

  private lazy val csrf = injector.instanceOf[Csrf]
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val jdAdUtil = injector.instanceOf[JdAdUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val streamsUtil = injector.instanceOf[StreamsUtil]
  private lazy val mItems = injector.instanceOf[MItems]
  implicit private lazy val mat = injector.instanceOf[Materializer]


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
        implicit val lkCtxData2 = CtxData.jsInitTargetsAppendOne( MJsInitTargets.LkAdsForm )(lkCtxData0)
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
    import esModel.api._

    // TODO Добавить поддержку агрумента mode
    lazy val logPrefix = s"getAds(${nodeKey.mkString("/")}+$offset1${newAdIdOpt.fold("")("," + _)})#${System.currentTimeMillis()}:"

    // Нужно найти запрашиваемые карточки:
    val parentNodeId = nodeKey.last

    // Макс кол-во карточек за один запрос.
    val maxAdsPerTime = LkAdsFormConst.GET_ADS_COUNT_PER_REQUEST

    // Быстро узнать id карточек, которые надо отобразить на экране:
    val madIdsFut = mNodes.dynSearchIds(
      new MNodeSearch {
        override val nodeTypes = MNodeTypes.Ad :: Nil
        override val outEdges: MEsNestedSearch[Criteria] = {
          // Поиск по узлу-владельцу
          val crOwn = Criteria(
            predicates  = MPredicates.OwnedBy :: Nil,
            nodeIds     = parentNodeId :: Nil
          )
          MEsNestedSearch.plain( crOwn )
        }
        override def limit     = maxAdsPerTime
        override val withDateCreatedSort = Some(SortOrder.DESC)
        override def offset    = offset1
      }
    )

    // Возможна ситуация, что есть доп.карточка, которая не видна в поиске. Такое возможно,
    // если создать карточку в редакторе, сохранить её и сразу отредиректить юзера в список карточек.
    val createdAdOptFut = FutureUtil.optFut2futOpt(newAdIdOpt) { newAdId =>
      val headAdOrExFut = for {
        // Дождаться текущиго списка карточек:
        madIds <- madIdsFut
        // Узнать, есть ли карточка в этом списке?
        if !madIds.contains( newAdId )
        // Нет в списке, значит это свеже-созданная карточка, которую надо прочитать из БД и добавить в началов списка карточек:
        newAdOpt <- mNodes
          .getByIdCache(newAdId)
          .withNodeType(MNodeTypes.Ad)
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


    import slick.profile.api._
    import streamsUtil.Implicits._

    // Поиск данных по размещениям в базе биллинга:
    val madId2advStatusesMapFut = madIdsFut.flatMap { madIds =>
      LOGGER.trace(s"$logPrefix madIds[${madIds.length}/$maxAdsPerTime] = ${madIds.mkString(", ")}")

      slick.db
        .stream(
          // TODO Надо отрабатывать ошибки. При sql-ошибках они не вызывают HTTP 500, не логгируются, сбивая с толку.
          mItems
            .findStatusesForAds(
              // Не добавляем сюда newAdId, т.к. если у только что созданной карточки размещений быть и не должно.
              adIds    = madIds,
              statuses = MItemStatuses.advBusy.to( Iterable )
            )
            .forPgStreaming(30)
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
      .future( madIdsFut )
      .flatMapConcat( mNodes.multiGetSrc(_) )

    // Запихнуть свеже-созданную карточку newAdId в начало общего Source, если есть такая карточка.
    val allAdsSrc = newAdIdOpt.fold[Source[MNode, _]](normalAdsSrc) { _ =>
      Source.futureSource {
        for (createdAdOpt <- createdAdOptFut) yield {
          LOGGER.trace(s"$logPrefix Newly created ad: ${createdAdOpt.flatMap(_.id)}")
          createdAdOpt.fold[Source[MNode, _]](normalAdsSrc) { newAd =>
            normalAdsSrc.prepend( Source.single(newAd) )
          }
        }
      }
    }

    // Рендер карточек очень обычный и простой.
    val jdConf = MJdConf.simpleMinimal

    // Рендерим карточки в потоке:
    val adsRenderedSrc = allAdsSrc
      .mapConcat { mad =>
        val r = (for {
          doc <- mad.extras.doc
          mainRes <- doc.template.getMainBlockOrFirst()
        } yield {
          mainRes -> mad
        })

        if (r.isEmpty)
          LOGGER.error(s"$logPrefix Skip render of node#${mad.idOrNull} ntype=${mad.common.ntype}: empty document or zero-blocks jd-tree: ${mad.extras.doc}")

        r.toList
      }
      // Параллельно рендерим запрошенные карточки:
      .mapAsync(8) { case ((mainTpl, mainBlkIndex), mad) =>
        LOGGER.trace(s"$logPrefix Will render ${mad.idOrNull}")
        // Убрать wide-флаг в main strip'е, иначе будет плитка со строкой-дыркой.
        val mainNonWideTpl = jdAdUtil.resetBlkWide( mainTpl )
        val edges2 = jdAdUtil.filterEdgesForTpl(mainNonWideTpl, mad.edges)

        val jdAdDataFut = jdAdUtil.mkJdAdDataFor
          .show(
            nodeId        = mad.id,
            nodeEdges     = edges2,
            tpl           = mainNonWideTpl,
            // Тут по идее надо четверть или половину, но с учётом плотности пикселей можно округлить до 1.0. Это и нагрузку снизит.
            jdConf        = jdConf,
            allowWide     = false,
            selPathRev    = mainBlkIndex :: List.empty,
            // title: сейчас в форме названия нигде не рендерятся. Поэтому и на клиент их не отправляем.
            nodeTitle     = None,
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
          LOGGER.trace(s"$logPrefix Done render ${mad.idOrNull}, ${jdAdData.edges.size} edges, rootJdt=${jdAdData.doc.template.rootLabel}, shownAtParent?$shownAtParent")
          (mad.id, jdAdData, shownAtParent)
        }
      }

    // Поток отрендеренного JSON'а, который уже готов для отправки клиенту:
    val adsSrcJsonBytes = Source.futureSource {
      for {
        madId2advStatusesMap <- madId2advStatusesMapFut
      } yield {
        LOGGER.trace(s"$logPrefix Statuses map[${madId2advStatusesMap.size}]: ${madId2advStatusesMap.mkString(" | ")}")
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
