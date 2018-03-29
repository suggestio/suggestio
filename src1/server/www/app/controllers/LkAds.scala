package controllers

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import io.suggest.ads.{LkAdsFormConst, MLkAdsFormInit, MLkAdsOneAdAdvForm, MLkAdsOneAdResp}
import io.suggest.adv.decl.{MAdvDeclKey, MAdvDeclKv, MAdvDeclSpec}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.IMust
import io.suggest.init.routed.MJsiTgs
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MAdItemStatuses, MItems}
import io.suggest.model.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.mctx.Context
import models.mlk.MNodeAdInfo
import models.mproj.ICommonDi
import org.elasticsearch.search.sort.SortOrder
import play.api.libs.json.{JsArray, JsString, Json}
import util.acl.{CanUpdateSls, IsAuth, IsNodeAdmin}
import util.ad.JdAdUtil
import views.html.lk.ads._
import japgolly.univeq._
import play.api.http.HttpEntity
import play.api.mvc.Result
import util.ads.LkAdsFormUtil
import util.adv.direct.AdvRcvrsUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 18:21
  * Description: Менеджер карточек в личном кабинете.
  * React-замена MarketLkAdn.showNodeAds() и связанным экшенам.
  */
class LkAds @Inject() (
                        isNodeAdmin             : IsNodeAdmin,
                        canUpdateSls            : CanUpdateSls,
                        advRcvrsUtil            : AdvRcvrsUtil,
                        isAuth                  : IsAuth,
                        jdAdUtil                : JdAdUtil,
                        lkAdsFormUtil           : LkAdsFormUtil,
                        mNodes                  : MNodes,
                        streamsUtil             : StreamsUtil,
                        mItems                  : MItems,
                        override val mCommonDi  : ICommonDi
                      )
  extends SioController
  with MacroLogsImpl
{

  import mCommonDi._
  import streamsUtil.Implicits._
  import io.suggest.scalaz.ScalazUtil.Implicits._
  import mNodes.Implicits._


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
            adIds = madIds,
            statuses = MNodeAdInfo.statusesSupported.toSeq
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


  /** Чтение информпации по внутренним размещениям указанной карточки.
    *
    * @param adKey Ключ узла-карточки.
    * @return 200 OK + adv.decls json
    */
  /*
  def getAdv(adKey: RcvrKey) = canUpdateSls( adKey.last ).async { implicit request =>

      // TODO Собрать advDecl на основе узлов и эджей карточки.
      // TODO Собрать отображаемые в форме метаданные по узлам.

    // TODO Искать подкарточки?

    val rcvrSelfPred = MPredicates.Receiver.Self
    val rcvrSelfItype = MItemTypes.AdvDirect
    for {
      e <- request.mad.edges
        .withPredicateIter( rcvrSelfPred )
      nodeId <- e.nodeIds
    } yield {
      MAdvDeclKv(
        key = MAdvDeclKey(
          itype = rcvrSelfItype,
          // TODO Тут надо нормальный rcvrKey генерить как-то: идти от узла вверх по цепочке предикатов OwnedBy в поисках текущего юзера.
          rcvrKey = Some( nodeId :: Nil )
        ),
        spec = MAdvDeclSpec(
          isShowOpened = e.info.flag,
          advPeriod    = None
        )
      )
    }
    request.mad.edges
      .withPredicateIter( MPredicates.Receiver.Self )
      .map { edge =>

      }
    ???
  }
  */


  /** Сохранить настройки размещения для указанной карточки.
    * В request.body содержится MLkAdsOneAdAdvForm в виде JSON.
    *
    * @param adKey Цепочка узлов до рекламной карточки.
    * @return JSON-ответ с обновлёнными данными размещения карточки.
    */
  /*
  def setAdv(adKey: RcvrKey) = csrf.Check {
    canUpdateSls( adKey.last ).async(parse.json[MLkAdsOneAdAdvForm]) { implicit request =>
      lazy val logPrefix = s"setAdv(${request.mad.idOrNull})#${System.currentTimeMillis()}:"
      LOGGER.trace(s"$logPrefix Starting, adKey=${adKey.mkString(" / ")}")

      lkAdsFormUtil.oneAdAdvFormVld( request.body ).fold(
        // Зафейлилась раняя проверка входных данных
        {failures =>
          LOGGER.warn(s"$logPrefix Failed to validate form:\n ${failures.iterator.mkString("\n ")}")
          // Вернуть список проблем в виде JSON назад клиенту
          val json = JsArray(
            failures
              .iterator
              .map(JsString.apply)
              .toIndexedSeq
          )
          NotAcceptable( json )
        },
        // Синхронные проверки валидности данных пройдены, надо сверить запрос с данными системы.
        {form =>
          LOGGER.trace(s"$logPrefix Form vld passed: ${form.decls.length} decls, form = $form")

          // Надо проверить adv-ключи на доступность для обработки.
          // Надо собрать всех ресиверов, проверить права юзера на управление размещением в них (требуем node admin права):
          val allRcvrKeys = form.decls
            .iterator
            .flatMap(_.key.rcvrKey)
            .toSet
          LOGGER.trace(s"$logPrefix ${allRcvrKeys.size} rcvrs ")

          // Любые нетривиальные бесплатные размещения надо проводить через биллинг. Тривиальные прямые размещения - обычно ставятся напрямую, т.к. они пока без dateEnd.
          val (unbilled, billed) = form.decls.partition { declKv =>
            declKv.spec.advPeriod.isEmpty &&
              (declKv.key.itype ==* MItemTypes.AdvDirect)
          }
          LOGGER.trace(s"$logPrefix ${unbilled.size} unbilled + ${billed.size} BILLed advs")

          // TODO Надо дореализовать внутренние размещения, идущие через биллинг. Они уже в различных биллингах, и уже работают в LkAdvGeo.
          if (billed.nonEmpty)
            throw new UnsupportedOperationException(s"$logPrefix Billed advs not implemented here")

          // Запустить проверки доступа для всех перечисленных в спеке узлов. Перегонка в Iterable для явного Iterable-выхлопа
          val rcvrChecksFut = for {
            rcvrsCheckResults <- Future.traverse( allRcvrKeys: Iterable[RcvrKey] ) { nodeKey =>
              isNodeAdmin.isNodeChainAdmin(nodeKey, request.user)
            }
            // Проверяем, что все права доступа на узлу действительно валидны:
            if {
              val r = rcvrsCheckResults.forall(_.isDefined)
              if (!r)
                LOGGER.warn(s"$logPrefix ${rcvrsCheckResults.count(_.isEmpty)} of ${rcvrsCheckResults.size} rcvr checks FAILED, ${rcvrsCheckResults.count(_.isDefined)} passed. Failed rcvr keys were: \n${allRcvrKeys.iterator.zip(rcvrsCheckResults.iterator).filter(_._2.isEmpty).map(_._1).mkString("\n ")}")
              r
            }
          } yield {
            LOGGER.trace(s"$logPrefix All ${rcvrsCheckResults.size} rcvrs adv checks passed")
            rcvrsCheckResults
          }

          for {
            // Когда все проверки выполнены, можно начать применять все запрошенные изменения
            _ <- rcvrChecksFut

            // Заливка возможных unbilled-размещений прямо в рекламную карточку
            mad2 <- if (unbilled.nonEmpty) {
              LOGGER.trace(s"$logPrefix Checks done. Have unbilled adv.decls. Will apply unbilled...")
              // Есть unbilled-размещения. Реорганизовать данные этих размещений и накатить их.
              val rcvrSelfPred = MPredicates.Receiver.Self

              // Собрать все новые эджи
              val newEdges = unbilled
                .groupBy(_.unbilledEdgesGroupingKey)
                // Не используем mapValues(), т.к. он через-чур ленив и не даёт доступа к ключам.
                .iterator
                .map { case ((itype, showOpenedOpt), edgeDeclGroup) =>
                  // Предикат отрабатывается первым, чтобы возможная ошибка была как можно раньше.
                  val predicate = itype match {
                    case MItemTypes.AdvDirect =>
                      rcvrSelfPred
                    case other =>
                      throw new UnsupportedOperationException(s"$logPrefix Unbilled item type $other is not supported as predicate. Check src.code for unbilled advs.")
                  }
                  val nodeIds = {
                    val iter = for {
                      declKv  <- edgeDeclGroup
                      rcvrKey <- declKv.key.rcvrKey
                    } yield {
                      rcvrKey.last
                    }
                    iter.toSet
                  }
                  val edgeFlagOpt = showOpenedOpt
                    .flatMap(OptionUtil.maybeTrue)
                  val edgeInfo = edgeFlagOpt
                    .fold(MEdgeInfo.empty)(_ => MEdgeInfo(flag = edgeFlagOpt))
                  val e = MEdge(
                    predicate = predicate,
                    nodeIds = nodeIds,
                    info = edgeInfo
                  )
                  LOGGER.trace(s"$logPrefix Unbilled edge: $e")
                  e
                }
                .toStream

              // Запуск обновлялки карточки:
              mNodes.tryUpdate( request.mad ) { mad =>
                mad.withEdges(
                  mad.edges.copy(
                    out = MNodeEdges.edgesToMap1(
                      mad.edges
                        // Удалить (перезаписать) все старые небиллингуемые эджи.
                        .withoutPredicateIter( rcvrSelfPred )
                        .++( newEdges )
                        .toSeq
                    )
                  )
                )
              }

            } else {
              LOGGER.trace(s"$logPrefix No unbilled adv.decls.")
              Future.successful(request.mad)
            }


          } yield {
            LOGGER.trace(s"$logPrefix Ok, applied adv-changes to ad#${request.mad.idOrNull}")

            // TODO Вернуть обновлённые данные по карточке
            Ok
          }
        }
      )
    }
  }
  */

}
