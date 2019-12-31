package util.compat

import java.util.concurrent.atomic.AtomicInteger

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink}
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.{BulkProcessorListener, EsModel, IMust}
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.media.{MEdgeMedia, MMedia, MMedias}
import io.suggest.model.n2.media.search.MMediaSearchDfltImpl
import io.suggest.model.n2.media.storage.{MStorageInfo, MStorageInfoData, MStorages}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.util.JmxBase
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import javax.inject.Inject
import models.im.MImg3
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.12.2019 12:58
  * Description: Код для переноса данных из старой MMedia в MNode + File edge.
  */
final class Media2NodeMigration @Inject() (
                                            esModel   : EsModel,
                                            mMedias   : MMedias,
                                            mNodes    : MNodes,
                                            implicit private val mat: Materializer,
                                            implicit private val ec: ExecutionContext,
                                          )
  extends MacroLogsImpl
{

  import esModel.api._

  /** Запуск процедуры миграции media на media-узлы. */
  def migrateMediasToNodes(): Future[Long] = {
    val logPrefix = s"migrateMediasToNodes()#${System.currentTimeMillis()}"

    for {

      // Сборка карты текущих узлов-оригиналов в памяти:
      origNodesMap <- mNodes
        .source[MNode](
          searchQuery = new MNodeSearchDfltImpl {
            override val nodeTypes = MNodeTypes.Media.Image :: Nil
            override val outEdges: Seq[Criteria] = {
              // Защита от повторного запуска: НЕ выкачивать узлы картинок-деривативов, грабастать только оригиналы (без File-эджей или )
              val cr = Criteria(
                predicates      = MPredicates.File :: Nil,
                fileIsOriginal  = Some(false),
                must            = IMust.MUST_NOT,
              )
              cr :: Nil
            }
          }
            .toEsQuery
        )( mNodes.Implicits.elSourcingHelper )
        .map { mnode =>
          mnode.id.get -> mnode
        }
        .toMat(
          Sink.collection[(String, MNode), Map[String, MNode]]
        )(Keep.right)
        .run()

      bp = {
        LOGGER.debug(s"$logPrefix Collected ${origNodesMap.size} pre-existing img-nodes.")
        mNodes.bulkProcessor(
          listener = BulkProcessorListener( logPrefix + "-BULK:"),
        )
      }

      nodesCounter = new AtomicInteger()

      // И наконец, запуск обработки MMedia:
      _ <- mMedias
        .source[MMedia](
          searchQuery = (new MMediaSearchDfltImpl).toEsQuery,
        )( mMedias.Implicits.elSourcingHelper )
        .runForeach { mmedia =>
          val mimg = MImg3( mmedia )

          val fileEdge = MEdge(
            predicate = MPredicates.File,
            media = Some(MEdgeMedia(
              storage = MStorageInfo(
                storage = MStorages.SeaWeedFs,
                data = MStorageInfoData(
                  data = mmedia.storage.fid.toString,
                ),
              ),
              file = mmedia.file,
              picture = mmedia.picture,
            )),
            nodeIds = mimg.dynImgId
              .maybeOriginal
              .map(_.mediaId)
              .toSet,
          )

          val mnode = OptionUtil
            .maybeOpt( mmedia.file.isOriginal ) {
              // Не знаю зачем, но на всякий случай пытаемся возможно-разные id. Скорее всего оба id узла-оригинала всегда одинаковые.
              Set( mmedia.nodeId, mimg.dynImgId.mediaId )
                .iterator
                .flatMap( origNodesMap.get )
                .nextOption()
            }
            .fold[MNode] {
              LOGGER.trace(s"$logPrefix Will create node#${mimg.dynImgId.mediaId}")
              MNode(
                id = Some( mimg.dynImgId.mediaId ),
                common = MNodeCommon(
                  ntype       = MNodeTypes.Media.Image,
                  isDependent = true,
                ),
                edges = MNodeEdges(
                  out = MNodeEdges.edgesToMap( fileEdge ),
                ),
              )
            } {
              // Уже есть нода для текущей картинки-оригинала. Пропатчить...
              MNode.edges.modify { edges0 =>
                MNodeEdges.out.set(
                  MNodeEdges.edgesToMap1(
                    edges0.withoutPredicateIter( MPredicates.File ) ++ (fileEdge :: Nil)
                  )
                )(edges0)
              }
            }

          bp.add(
            mNodes
              .prepareIndex( mnode )
              .request()
          )
          LOGGER.trace(s"$logPrefix Processed node#${mnode.idOrNull} - ${mmedia.storage.fid.toString}")

          nodesCounter.incrementAndGet()
        }
        .andThen { case _ =>
          LOGGER.trace(s"$logPrefix Closing bulk processor after ${nodesCounter.get()} items")
          bp.close()
        }

    } yield {
      val r = nodesCounter.get()
      LOGGER.info(s"$logPrefix Migrated $r mmedias to nodes")
      r
    }

  }


  /** Удаление всех MMedia из БД.
    * Используется после успешной миграции mmedia -> mnode (см. выше).
    *
    * @return Фьючерс.
    */
  def purgeAllMMedias(): Future[Int] = {
    mMedias.deleteByQuery(
      mMedias.startScroll(
        queryOpt          = Some( new MMediaSearchDfltImpl().toEsQuery ),
      )
    )
  }

}


// Поддержка JMX.

sealed trait Media2NodeMigrationJmxMBean {
  def migrateMediasToNodes(): String
  def purgeAllMMedias(): String
}

final class Media2NodeMigrationJmx @Inject() (
                                               injector: Injector,
                                             )
  extends JmxBase
  with Media2NodeMigrationJmxMBean
  with MacroLogsDyn
{

  implicit private def _ec = injector.instanceOf[ExecutionContext]
  implicit private def media2NodeMigration = injector.instanceOf[Media2NodeMigration]

  override def _jmxType = JmxBase.Types.COMPAT

  override def migrateMediasToNodes(): String = {
    val logPrefix = "migrateMediasToNodes()"
    LOGGER.info( s"$logPrefix Starting" )
    val fut = for {
      countMigrated <- media2NodeMigration.migrateMediasToNodes()
    } yield {
      val msg = s"Migrated $countMigrated medias."
      LOGGER.info(s"$logPrefix $msg")
      msg
    }
    JmxBase.awaitString( fut )
  }

  override def purgeAllMMedias(): String = {
    val logPrefix = "purgeAllMMedias()"
    LOGGER.warn(s"$logPrefix Deleting all mmedia...")
    val fut = for {
      countDeleted <- media2NodeMigration.purgeAllMMedias()
    } yield {
      val msg = s"Deleted $countDeleted medias."
      LOGGER.info(s"$logPrefix $msg")
      msg
    }
    JmxBase.awaitString( fut )
  }

}
