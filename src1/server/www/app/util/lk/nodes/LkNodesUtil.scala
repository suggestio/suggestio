package util.lk.nodes

import com.google.inject.{Inject, Singleton}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import org.elasticsearch.search.sort.SortOrder

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 13:25
  * Description: Утиль для LkNodes -- системы управления узлами в личном кабинете.
  */
@Singleton
class LkNodesUtil @Inject() (
                             mCommonDi : ICommonDi
                            )
  extends MacroLogsImpl
{

  def SUB_NODES_LIMIT = 40

  def subNodesSearch(nodeId: String, offset1: Int = 0): MNodeSearch = {
    new MNodeSearchDfltImpl {
      // Если вдруг понадобиться больше, то браузер должен будет подгрузить ещё отдельным запросом.
      override def limit  = SUB_NODES_LIMIT
      override def offset = offset1

      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(
          nodeIds     = nodeId :: Nil,
          predicates  = MPredicates.OwnedBy :: Nil
        )
        cr :: Nil
      }
      override def withNameSort = Some( SortOrder.ASC )
      override def nodeTypes = MNodeTypes.BleBeacon :: Nil
    }
  }

}
