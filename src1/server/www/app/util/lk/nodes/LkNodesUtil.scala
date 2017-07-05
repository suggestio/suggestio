package util.lk.nodes

import javax.inject.Singleton
import io.suggest.adn.edit.NodeEditConstants
import io.suggest.ble.BeaconUtil
import io.suggest.lk.nodes.MLknNodeReq
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.search.sort.SortOrder
import util.FormUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 13:25
  * Description: Утиль для LkNodes -- системы управления узлами в личном кабинете.
  */
@Singleton
class LkNodesUtil
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


  import com.wix.accord.dsl._

  /** Валидация id узла. */
  //private val nodeIdV = validator[String] { esId =>
  //  FormUtil.isEsIdValid(esId) should equalTo(true)
  //}

  /** Валидация id маячка. Ожидается строка в нижнем регистре. */
  private val eddyStoneIdV = validator[String] { esId =>
    esId should matchRegexFully( BeaconUtil.EddyStone.EDDY_STONE_NODE_ID_RE_LC )
  }

  /** Валидация названия узла. */
  private val nodeNameV = {
    val n = NodeEditConstants.Name
    validator[String] { raw =>
      raw.length should be >= n.LEN_MIN
      raw.length should be <= n.LEN_MAX
    }
  }

  private val mLknNodeReqEditV = validator[MLknNodeReq] { req =>
    req.name.should(nodeNameV)
    req.id should empty   // Нельзя редактировать id, хотя в модели запроса это поле присутствует.
  }

  private val mLknNodeReqCreateV = validator[MLknNodeReq] { req =>
    req.name.should(nodeNameV)
    req.id should notEmpty  // На первом этапе можно добавлять только маячки, а они только с id.
    req.id.each.should(eddyStoneIdV)
    //req.parentId.each.should(nodeIdV)
  }


  import com.wix.accord._

  def validateNodeReq(nodeInfo: MLknNodeReq, isEdit: Boolean): Either[Set[Violation], MLknNodeReq] = {
    val nodeInfo1 = nodeInfo.copy(
      name  = FormUtil.strTrimSanitizeF(nodeInfo.name),
      id    = nodeInfo.id.map( FormUtil.strTrimSanitizeLowerF )
    )
    val v = if (isEdit) mLknNodeReqEditV else mLknNodeReqCreateV
    validate(nodeInfo1)(v) match {
      case Success =>
        Right(nodeInfo1)
      case Failure(res) =>
        Left(res)
    }
  }

}
