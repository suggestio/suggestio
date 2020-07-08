package util.lk.nodes

import io.suggest.es.model.MEsNestedSearch
import io.suggest.lk.nodes.MLknNodeReq
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.MNodeTypes
import io.suggest.n2.node.search.MNodeSearch
import org.elasticsearch.search.sort.SortOrder
import util.FormUtil
import scalaz._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 13:25
  * Description: Утиль для LkNodes -- системы управления узлами в личном кабинете.
  */
final class LkNodesUtil {

  def SUB_NODES_LIMIT = 40

  def subNodesSearch(nodeId: String, offset1: Int = 0): MNodeSearch = {
    new MNodeSearch {
      // Если вдруг понадобиться больше, то браузер должен будет подгрузить ещё отдельным запросом.
      override def limit  = SUB_NODES_LIMIT
      override def offset = offset1

      override val outEdges: MEsNestedSearch[Criteria] = {
        val cr = Criteria(
          nodeIds     = nodeId :: Nil,
          predicates  = MPredicates.OwnedBy :: Nil
        )
        MEsNestedSearch(
          clauses = cr :: Nil,
        )
      }
      override def withNameSort = Some( SortOrder.ASC )
      override def nodeTypes = MNodeTypes.BleBeacon :: Nil
    }
  }


  def validateNodeReq(nodeInfo: MLknNodeReq, isEdit: Boolean): ValidationNel[String, MLknNodeReq] = {
    val nodeInfo1 = nodeInfo.copy(
      name  = FormUtil.strTrimSanitizeF(nodeInfo.name),
      id    = nodeInfo.id.map( FormUtil.strTrimSanitizeLowerF )
    )
    MLknNodeReq.validateReq( nodeInfo1, isEdit )
  }

}
