package util.up

import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicInteger

import akka.stream.Materializer
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.{BulkProcessorListener, EsModel, IMust, MEsNestedSearch}
import io.suggest.n2.edge.{MEdgeFlags, MPredicates}
import io.suggest.n2.edge.search.{Criteria, EdgeFlagCriteria, EsRange, EsRangeClause, EsRangeOps}
import io.suggest.n2.media.storage.IMediaStorages
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.util.logs.MacroLogsDyn
import javax.inject.Inject
import models.im.{MDynImgId, MLocalImg, MLocalImgs}
import models.mcron.MCronTask
import play.api.Configuration
import play.api.inject.Injector
import util.cron.ICronTasksProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.07.2020 15:43
  * Description: Чистка повисших загрузок файлов.
  */
final class CleanStaleUploads @Inject() (
                                          injector: Injector,
                                        )
  extends ICronTasksProvider
  with MacroLogsDyn
{

  private def configuration = injector.instanceOf[Configuration]


  /** Через сколько дней удалять повисшие закачки? */
  def RM_STALE_AFTER_DAYS: Int = {
    configuration
      .getOptional[Int]("upload.stale.delete.days")
      .getOrElse(2)
  }


  /** Список задач, которые надо вызывать по таймеру. */
  override def cronTasks(): Iterable[MCronTask] = {
    val cleanUpTask = MCronTask(
      startDelay  = 30.seconds,
      every       = 6.hours,
      displayName = getClass.getSimpleName + ".cleanUpStale()"
    )(cleanUpStale)

    cleanUpTask :: Nil
  }


  /** Поиск и зачистка данных на текущем хосте.
    *
    * @return Фьючерс.
    */
  def cleanUpStale(): Future[_] = {
    val mNodes = injector.instanceOf[MNodes]
    implicit val ec = injector.instanceOf[ExecutionContext]
    val esModel = injector.instanceOf[EsModel]
    val uploadUtil = injector.instanceOf[UploadUtil]
    lazy val iMediaStorages = injector.instanceOf[IMediaStorages]
    lazy val mLocalImgs = injector.instanceOf[MLocalImgs]
    implicit val mat = injector.instanceOf[Materializer]

    val log = LOGGER
    val bpListener = BulkProcessorListener( getClass.getSimpleName )

    import esModel.api._

    val bp = mNodes.bulkProcessor( bpListener )
    val counter = new AtomicInteger(0)

    // Поискать узлы, которые залежались в недокачанном состоянии.
    mNodes
      .dynSearchSource(
        new MNodeSearch {
          override val outEdges: MEsNestedSearch[Criteria] = {
            val must = IMust.MUST

            val myNodeFileCr = Criteria(
              nodeIds = uploadUtil.MY_NODE_PUBLIC_HOST :: Nil,
              predicates = MPredicates.Blob :: Nil,
              flags = EdgeFlagCriteria(
                flag = MEdgeFlags.InProgress :: Nil,
              ) :: Nil,
              must = must,
              // Профильтровать на предмет залежавшейся даты:
              date = EsRange(
                rangeClauses = EsRangeClause
                  .op( EsRangeOps.`<` )
                  .value( OffsetDateTime.now().minusDays(RM_STALE_AFTER_DAYS) ) :: Nil,
                must = must,
              ) :: Nil,
            )

            MEsNestedSearch( myNodeFileCr :: Nil )
          }

          override val nodeTypes = MNodeTypes.Media.children
          // Узлы на начало upload'а создаются неактивными. Если узел активен, то тут что-то по-сложнее.
          override val isEnabled = Some(false)
        }
      )
      // И пройтись по узлам, зачистить storage и остальное.
      .runForeach { mnode =>
        val fileEdgeOpt = mnode.edges
          .withPredicateIter( MPredicates.Blob.File )
          .nextOption()
        val nodeId = mnode.id.get

        log.info(s"Deleting node#$nodeId with stale upload: ${fileEdgeOpt.orNull}")

        val locImg = MLocalImg( MDynImgId(nodeId) )
        val deleteLocalFut = mLocalImgs.deleteAllFor( locImg.dynImgId.mediaId )
        for (ex <- deleteLocalFut.failed)
          log.warn(s"Failed to delete localImg#${locImg}", ex)

        val storageOpt = fileEdgeOpt
          .flatMap(_.media)
          .flatMap(_.storage)

        // Зачистить file storage, если есть.
        val deleteStorageFut = FutureUtil.optFut2futOpt( storageOpt ) { storage =>
          iMediaStorages
            .client( storage.storage )
            .delete( storage.data )
            .map( Some.apply )
        }
        for (ex <- deleteStorageFut.failed)
          log.error(s"Failed to delete from storage $storageOpt", ex)

        // И удалить сам узел:
        bp.add(
          mNodes.prepareDelete( nodeId ).request()
        )

        counter.incrementAndGet()
      }
      // Залоггировать суть:
      .andThen { _ =>
        bp.close()
        val deletedCount = counter.get()
        val msg = s"Deleted $deletedCount stale-upload nodes."
        if (deletedCount > 0)
          LOGGER.info(msg)
        else
          LOGGER.trace(msg)
      }
  }

}
