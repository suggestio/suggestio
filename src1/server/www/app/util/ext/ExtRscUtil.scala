package util.ext

import java.net.URL

import io.suggest.es.model.EsModel
import io.suggest.es.util.{IEsClient, SioEsUtil}
import io.suggest.es.util.SioEsUtil.EsActionBuilderOpsExt
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.extra.MNodeExtras
import io.suggest.model.n2.extra.rsc.{MHostNameIndexed, MRscExtra}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.text.util.UrlUtil
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.vid.ext.{MVideoExtInfo, VideoExtUrlParsers}
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 14:09
  * Description: Утиль для работы с внешними информационными ресурсами.
  * (которые можно встраивать в рекламные карточки через фрейм).
  */
@Singleton
class ExtRscUtil @Inject()(
                            esModel   : EsModel,
                            mNodes    : MNodes,
                            implicit private val ec: ExecutionContext,
                            esClientP: IEsClient,
                          )
  extends MacroLogsImpl
{

  import esClientP.esClient
  import esModel.api._

  /** Взять ссылки, вернуть узлы для ссылок.
    * Все операции пакетные для ускорения при множестве результатов.
    *
    * @param videoUrls Ссылки на внешние frame-ресурсы (вкл.видео).
    * @param personIdOpt id текущего юзера, если есть (для указания эджа CreatedBy на новых/создаваемых узлах).
    *
    * @return Карта из исходных ссылок и созданных узлов.
    */
  def ensureExtRscNodes(videoUrls: Iterable[String], personIdOpt: Option[String]): Future[Map[String, MNode]] = {
    if (videoUrls.isEmpty) {
      Future.successful(Map.empty)
    } else {
      _ensureExtRscNodes( videoUrls, personIdOpt )
    }
  }

  private def _ensureExtRscNodes(videoUrls: Iterable[String], personIdOpt: Option[String]): Future[Map[String, MNode]] = {
    lazy val logPrefix = s"ensureExtVideoNodes(${videoUrls.size}u)[${System.currentTimeMillis}]:"
    LOGGER.trace(s"$logPrefix Ensuring following video URLs:\n ${videoUrls.mkString("\n ")}")

    // Распарсить все ссылки на внешние видео и остальное, завернуть в id-узлов, поискать/создать эти узлы.
    // Для ускорения, делаем всё максимально пакетно.
    val videoParsers = new VideoExtUrlParsers
    val ntypes = MNodeTypes.ExternalRsc

    // Внутренний контейнер данных по узлу, который относится к ресурсу.
    case class RscInfo(
                        nodeType          : MNodeType,
                        remoteUrl         : String,
                        nodeRsc           : Option[MRscExtra],
                        nodeVideoExt      : Option[MVideoExtInfo],
                      )

    // Произвести распарсенные эквиваленты для ссылок:
    val rscNodeId2UrlsMap = videoUrls
      .iterator
      .map { remoteUrl =>
        val pr = videoParsers
          .parse( videoParsers.anySvcVideoUrlP, remoteUrl )

        // Сгенерить данные результат с выхлопа парсера:
        pr
          .map { ve =>
            // Успешно распарсена ссылка на внешнее видео.
            val nodeId = ve.toNodeId
            LOGGER.trace(s"$logPrefix Video-URL $remoteUrl => $pr")
            val r = RscInfo(
              nodeType        = ntypes.VideoExt,
              remoteUrl       = remoteUrl,
              nodeRsc         = None,
              nodeVideoExt    = Some(ve)
            )
            nodeId -> r
          }
          .getOrElse {
            // Это какой-то внешний ресурс (страничка, веб-приложение и т.д.), который юзер пихает во фрейм.
            val urlNorm = UrlUtil.normalize( remoteUrl )
            val urlNormC = new URL(urlNorm)
            val host = urlNormC.getHost
            val nodeId = resourceUrl2nodeId( urlNormC )
            LOGGER.trace(s"$logPrefix Just a resource:\n origURL = $remoteUrl\n normURL = $urlNorm\n nodeId = $nodeId")
            val r = RscInfo(
              nodeType        = ntypes.Resource,
              remoteUrl       = remoteUrl,
              nodeRsc         = Some( MRscExtra(
                url = urlNorm,
                hostNames = MHostNameIndexed(
                  host = host
                ) :: Nil,
                hostTokens = host.split('.').toSet
              )),
              nodeVideoExt    = None
            )
            nodeId -> r
          }
      }
      // Объединяем разные ссылки по одинаковым распарсенным данным видео.
      .toSeq
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2))

    LOGGER.trace(s"$logPrefix Detected ${rscNodeId2UrlsMap.size} ext-videos from ${videoUrls.size} video URLs")

    // Запустить поиск узлов, хранящих данные по запрошенным видео.
    val existVideoExtNodesSearch = new MNodeSearchDfltImpl {
      override def withIds    = rscNodeId2UrlsMap.keySet.toSeq
      // Фильтруем только по типу узла: если узел неожиданного типа живёт под ожидаемым id, пусть будет перезаписан.
      override def nodeTypes  = ntypes.children
      override def limit      = rscNodeId2UrlsMap.size
    }
    val existVideoNodesMapFut = mNodes.dynSearchMap( existVideoExtNodesSearch )

    // Досоздать недостающие узлы, когда будет известна инфа о существующих.
    for {

      existVideoNodesMap <- existVideoNodesMapFut

      // Нужно выяснить какие ноды уже существуют, а каких не хватает:
      missingVideoExtNodes = rscNodeId2UrlsMap
        .view
        .filterKeys { nodeId =>
          !(existVideoNodesMap contains nodeId)
        }

      allNodesMap <- if (missingVideoExtNodes.isEmpty) {
        LOGGER.trace(s"$logPrefix No need to create any video-ext nodes")
        Future.successful( existVideoNodesMap )

      } else {
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


        val bulk = esClient.prepareBulk()
        val newNodesByIdAcc = missingVideoExtNodes.foldLeft( List.empty[(String, MNode)] ) {
          case (acc0, (nodeId, infos)) =>
            // Дедубликация кода извлечения первого заданного поля из списка RscInfo.
            def __getInfoField[T](f: RscInfo => IterableOnce[T]): Option[T] = {
              infos
                .iterator
                .flatMap(f)
                .buffered
                .headOption
            }

            val mnode0 = MNode(
              common = MNodeCommon(
                ntype       = infos.head.nodeType,
                isDependent = true
              ),
              extras = MNodeExtras(
                extVideo = __getInfoField(_.nodeVideoExt),
                resource = __getInfoField(_.nodeRsc)
              ),
              // Без ссылки в meta, чтобы ссылка явно генерилась самостоятельно.
              edges = MNodeEdges(
                out = edges
              ),
              id = Some(nodeId)
            )
            val irb = mNodes.prepareIndex( mnode0 )
            bulk.add( irb.request )
            val mnode1 = (MNode.versionOpt set Some(SioEsUtil.DOC_VSN_0))(mnode0)

            (nodeId -> mnode1) :: acc0
        }

        // Запускаем сохранение новых узлов в БД.
        val saveFut = bulk.executeFut()

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
      }

    } yield {
      // Используя карту всех VideoExt-узлов, собрать итоговую карту ответа: ориг.ссылка -> MNode
      val resIter = for {
        (nodeId, mnode) <- allNodesMap.iterator
        rscInfos  <- rscNodeId2UrlsMap.get(nodeId).iterator
        rscInfo   <- rscInfos.iterator
      } yield {
        rscInfo.remoteUrl -> mnode
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


  /** Используя нормализованный URL, сгенерить id узла для ресурса.
    *
    * @param url Нормализованная ссылка на ресурс.
    * @return Строка с id узла для нормальной ссылки.
    */
  def resourceUrl2nodeId(url: URL): String = {
    // id узла для ссылки: префикс + dkey + остальная часть ссылки (path, qs, etc)
    "@" + UrlUtil.host2dkey(url.getHost) + url.getFile
  }

}

