package util.vid

import javax.inject.{Inject, Singleton}

import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.extra.MNodeExtras
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.es.util.SioEsUtil.laFuture2sFuture
import io.suggest.vid.ext.{MVideoExtInfo, VideoExtUrlParsers}
import models.mproj.ICommonDi

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 14:09
  * Description: Утиль для работы с видео.
  */
@Singleton
class VideoUtil @Inject() (
                            mNodes    : MNodes,
                            mCommonDi : ICommonDi
                          )
  extends MacroLogsImpl
{

  import mCommonDi._

  /** Взять ссылки, вернуть узлы для ссылок.
    * Все операции пакетные для ускорения при множестве результатов.
    *
    * @param videoUrls Ссылки на внешние embed-видео.
    * @param personIdOpt id текущего юзера, если есть (для указания эджа CreatedBy на новых/создаваемых узлах).
    *
    * @return Карта из исходных ссылок и созданных узлов.
    */
  def ensureExtVideoNodes(videoUrls: Iterable[String], personIdOpt: Option[String]): Future[Map[String, MNode]] = {
    if (videoUrls.isEmpty) {
      LOGGER.trace("ensureExtVideoNodes(): No video urls, skipping.")
      Future.successful(Map.empty)
    } else {
      _ensureExtVideoNodes( videoUrls, personIdOpt )
    }
  }

  private def _ensureExtVideoNodes(videoUrls: Iterable[String], personIdOpt: Option[String]): Future[Map[String, MNode]] = {
    lazy val logPrefix = s"ensureExtVideoNodes(${videoUrls.size}u)[${System.currentTimeMillis}]:"
    LOGGER.trace(s"$logPrefix Ensuring following video URLs:\n ${videoUrls.mkString("\n ")}")

    // Распарсить все ссылки на внешние видео, завернуть в id-узлов, поискать/создать эти узлы.
    // Для ускорения, делаем всё максимально пакетно.
    val parsers = new VideoExtUrlParsers

    // Произвести распарсенные эквиваленты для видео-ссылок:
    val extVid2Urls = videoUrls
      .iterator
      .map { videoUrl =>
        val pr = parsers
          .parse( parsers.anySvcVideoUrlP, videoUrl )

        if (pr.successful)
          LOGGER.trace(s"$logPrefix URL $videoUrl => $pr")
        else
          LOGGER.error(s"$logPrefix Failed to parse URL $videoUrl: $pr")

        // Пусть будет ошибка, если ссылка на видео некорректна: корректность должна проверяться на стадии валидации.
        val ve = pr.get
        ve -> videoUrl
      }
      // Объединяем разные ссылки по одинаковым распарасенным данным видео.
      .toSeq
      .groupBy(_._1)
      .mapValues(_.map(_._2))

    LOGGER.trace(s"$logPrefix Detected ${extVid2Urls.size} ext-videos from ${videoUrls.size} video URLs")

    // Пора поискать узлы, указывающие на данные видео.
    val nodeId2veMap = extVid2Urls
      .keysIterator
      .map { ve =>
        ve.toNodeId -> ve
      }
      .toMap

    val vidExtPred = MNodeTypes.ExternalRsc.VideoExt

    // Запустить поиск узлов, хранящих данные по запрошенным видео.
    val existVideoExtNodesSearch = new MNodeSearchDfltImpl {
      override def withIds    = nodeId2veMap.keys.toSeq
      // Фильтруем только по типу узла: если узел неожиданного типа живёт под ожидаемым id, пусть будет перезаписан.
      override def nodeTypes  = vidExtPred :: Nil
      override def limit      = nodeId2veMap.size
    }
    val existVideoNodesMapFut = mNodes.dynSearchMap( existVideoExtNodesSearch )

    // Досоздать недостающие узлы, когда будет известна инфа о существующих.
    for {

      existVideoNodesMap <- existVideoNodesMapFut

      // Нужно выяснить какие ноды уже существуют, а каких не хватает:
      missingVideoExtNodes = nodeId2veMap.filterKeys( nodeId => !existVideoNodesMap.contains(nodeId) )

      allNodesMap <- if (missingVideoExtNodes.nonEmpty) {
        LOGGER.info(s"$logPrefix Will create ${missingVideoExtNodes.size} nodes: [${missingVideoExtNodes.keysIterator.mkString(", ")}]")

        val edges: Seq[MEdge] = {
          val createdByEdgeOpt = for (personId <- personIdOpt) yield {
            MEdge(
              predicate = MPredicates.CreatedBy,
              nodeIds = Set(personId)
            )
          }
          createdByEdgeOpt.toList
        }

        val nodeCommon = MNodeCommon(
          ntype       = vidExtPred,
          isDependent = true
        )

        val bulk = esClient.prepareBulk()
        val newNodesByIdAcc = missingVideoExtNodes.foldLeft( List.empty[(String, MNode)] ) {
          case (acc0, (nodeId, veData)) =>
            val mnode0 = MNode(
              common = nodeCommon,
              extras = MNodeExtras(
                extVideo = Some(veData)
              ),
              // Без ссылки в meta, чтобы ссылка явно генерилась самостоятельно.
              edges = MNodeEdges(
                out = edges
              ),
              id = Some(nodeId)
            )
            val irb = mNodes.prepareIndex( mnode0 )
            bulk.add( irb.request )

            (nodeId -> mnode0.withFirstVersion) :: acc0
        }

        // Запускаем сохранение новых узлов в БД.
        val saveFut = bulk.execute()

        val newNodesByIdMap = newNodesByIdAcc.toMap
        saveFut
          .flatMap { brRes =>
            if (brRes.hasFailures) {
              // Удалить все созданные узлы.
              mNodes.deleteByIds(newNodesByIdMap.keys).onComplete {
                case Success(brResOpt)  => LOGGER.info(s"$logPrefix Deleted ${newNodesByIdMap.size} newly-created nodes due to previous errors.\n ${brResOpt.map(_.buildFailureMessage()).orNull}")
                case Failure(ex)        => LOGGER.error(s"$logPrefix Failed to delete ${newNodesByIdMap.size} newly-created nodes due to errors", ex)
              }

              // Были ошибки во время создания новых нод. Надо откатить всё назад.
              val logMsg = s"$logPrefix Cannot save all video-ext nodes: ${brRes.buildFailureMessage()}"
              LOGGER.error(logMsg)

              // Вернуть ошибку
              Future.failed( new IllegalStateException(logMsg) )

            } else {
              LOGGER.trace(s"$logPrefix Done saving ${newNodesByIdMap.size} new VE-nodes, took ${brRes.getTook}")
              val resMap = existVideoNodesMap ++ newNodesByIdMap
              Future.successful( resMap )
            }
          }

      } else {
        LOGGER.trace(s"$logPrefix No need to create any video-ext nodes")
        Future.successful( existVideoNodesMap )
      }

    } yield {
      // Используя карту всех VideoExt-узлов, собрать итоговую карту ответа: ориг.ссылка -> MNode
      val resIter = for {
        (nodeId, mnode) <- allNodesMap.iterator
        ve      <- nodeId2veMap.get(nodeId).iterator
        veUrls  <- extVid2Urls.get(ve).iterator
        veUrl   <- veUrls.iterator
      } yield {
        veUrl -> mnode
      }

      val resMap = resIter.toMap
      LOGGER.info(s"$logPrefix Done ensuring ${allNodesMap.size} video-ext-nodes from ${videoUrls.size} urls into result[${resMap.size}]:\n ${resMap.mkString("\n ")}")

      resMap
    }
  }


  /** Скомпилить данные внешнего видео в ссылку.
    *
    * @param videoExt Координаты видео на внешнем видео-сервисе.
    * @return Строка URL.
    */
  def toIframeUrl(videoExt: MVideoExtInfo): String = {
    videoExt.videoService
      .iframeSrc( videoExt.remoteId )
  }

}

