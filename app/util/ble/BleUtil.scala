package util.ble

import com.google.inject.Singleton
import io.suggest.common.radio.BeaconDistanceGroup_t
import io.suggest.model.es.IMust
import io.suggest.model.n2.edge.MPredicate
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.search.MSubSearch
import models.mgeo.MBleBeaconInfo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.10.16 16:01
  * Description: Утиль маячков в выдаче.
  */
@Singleton
class BleUtil {

  /**
    * Т.к. требуется вручную проставлять скоринг в зависимости от расстояния до маячка,
    * приходиться городить пачку подзапросов с ручными значениями скора для каждой distance-группы маячков.
    */
  def byBeaconGroupsSearches(topScore: Float, predicate: MPredicate, bcnGroups: Iterable[(BeaconDistanceGroup_t, Iterable[MBleBeaconInfo])]): Iterable[MSubSearch] = {
    val preds = Seq(predicate)
    for {
      (dstQuant, bcnsGrp)   <- bcnGroups
    } yield {
      val cr = Criteria(
        predicates  = preds,
        nodeIds     = bcnsGrp.iterator
          .map(_.uid)
          .toSet
          .toSeq
      )
      val _constScore = topScore / dstQuant
      val search = new MNodeSearchDfltImpl {
        override def outEdges     = Seq(cr)
        override def constScore   = Some(_constScore)
      }
      MSubSearch(
        search    = search,
        must      = IMust.SHOULD
      )
    }
  }

}


/** Интерфейс для поля с инжектируемым инстансом [[BleUtil]]. */
trait IBleUtilDi {
  def bleUtil: BleUtil
}
