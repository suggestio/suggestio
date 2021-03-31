package util.compat

import akka.stream.Materializer
import io.suggest.es.model.{BulkProcessorListener, EsModel, MEsNestedSearch}
import io.suggest.n2.edge.{MEdge, MEdgeFlagData, MEdgeFlags, MEdgeInfo, MNodeEdges, MPredicate, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImpl
import play.api.inject.Injector

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.2021 10:34
  * Description: Миграция эджей размещения с flag-boolean-поля на множество флагов.
  */
final class AdvAlwaysOpenedFlagIntoFlags @Inject() (
                                                     esModel                    : EsModel,
                                                     mNodes                     : MNodes,
                                                     implicit private val mat   : Materializer,
                                                     implicit private val ec    : ExecutionContext,
                                                   )
  extends MacroLogsImpl
{

  import esModel.api._
  import mNodes.Implicits._


  /** Провести миграцию с edge.info.flag для receiver-эджей на flags-множество.
    *
    * @return Фьючерс с результатом работы.
    */
  def migrateAll(): Future[String] = {
    val bp = mNodes.bulkProcessor(
      listener = BulkProcessorListener( getClass.getSimpleName ),
    )

    val node_edges_out_LENS = MNode.edges
      .composeLens( MNodeEdges.out )
    val preds = MPredicates.Receiver :: MPredicates.TaggedBy :: Nil
    val edge_info_LENS = MEdge.info
    val info_flag_LENS = MEdgeInfo.flag
    val info_flags_LENS = MEdgeInfo.flags
    val edge_info_flag_LENS = edge_info_LENS
      .composeLens( info_flag_LENS )
    val info_flag_unset_F = info_flag_LENS.set( None )
    val alwaysOpenedFd = MEdgeFlagData( MEdgeFlags.AlwaysOutlined )
    val info_flags_add_AlwaysOpened_F = info_flags_LENS.modify { infoFlags0 =>
      alwaysOpenedFd +: infoFlags0.toSeq
    }

    val nodesCountProcessed = new AtomicInteger()
    val edgesCountProcessed = new AtomicInteger()

    mNodes
      .source(
        searchQuery = new MNodeSearch {
          override val outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(
              predicates = preds,
              // Ищем только с флагом always opened:
              flag = Some(true),
            )
            MEsNestedSearch( cr :: Nil )
          }
        }.toEsQuery,
      )
      .mapAsyncUnordered(4) { mnode =>
        // Пройти и обновить эджи
        val mNode2 = node_edges_out_LENS.modify { edges0 =>
          for (e <- edges0) yield {
            if (
              preds.exists( e.predicate.eqOrHasParent ) &&
              edge_info_flag_LENS.exist(_.nonEmpty)(e)
            ) {
              // Требуется обработка данного эджа:
              LOGGER.trace(s"#${mnode.idOrNull} Updating edge $e")
              edgesCountProcessed.incrementAndGet()
              var infoModF = info_flag_unset_F

              if (!(e.info.flagsMap contains MEdgeFlags.AlwaysOutlined))
                infoModF = infoModF andThen info_flags_add_AlwaysOpened_F

              (edge_info_LENS modify infoModF)(e)
            } else {
              // Пропустить данный эдж.
              e
            }
          }
        }( mnode )

        bp add mNodes
          .prepareIndex(mNode2)
          .request()

        nodesCountProcessed.incrementAndGet()
        Future.successful(())
      }
      .run()
      .andThen( _ => bp.close() )
      .map { _ =>
        val msg = s"Processed $nodesCountProcessed nodes, $edgesCountProcessed edges updated."
        LOGGER.info(msg)
        msg
      }
  }

}


trait AdvAlwaysOpenedFlagIntoFlagsJmxMBean {
  def migrateAll(): String
}

final class AdvAlwaysOpenedFlagIntoFlagsJmx @Inject() (injector: Injector)
  extends JmxBase
  with AdvAlwaysOpenedFlagIntoFlagsJmxMBean
{

  private def advAlwaysOpenedFlagIntoFlags = injector.instanceOf[AdvAlwaysOpenedFlagIntoFlags]

  override def _jmxType = JmxBase.Types.COMPAT

  override def migrateAll(): String = {
    val fut = advAlwaysOpenedFlagIntoFlags.migrateAll()
    JmxBase.awaitString( fut )
  }

}
