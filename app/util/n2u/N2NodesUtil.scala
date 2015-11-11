package util.n2u

import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.ICriteria
import models.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.15 15:44
  * Description: Утиль для узлов N2.
  */
class N2NodesUtil {

  /** Попытаться узлать продьюсера узла. */
  def madProducerId(mad: MNode): Option[String] = {
    mad.edges
      .withPredicateIterIds( MPredicates.OwnedBy )
      .toStream
      .headOption
  }

  /** Попытаться узлать ресиверов узла. */
  def receiverIds(mad: MNode): Iterator[String] = {
    mad.edges
      .withPredicateIterIds( MPredicates.Receiver )
  }

  /** Попытаться узнать ресиверов в поисковых критериях эджей. */
  def receiverIds(crs: TraversableOnce[ICriteria]): Iterator[String] = {
    crs.toIterator
      .filter { _.predicates.contains(MPredicates.Receiver) }
      .flatMap( _.nodeIds )
  }

}


/** Интерфейс доступа к DI-полю. */
trait IN2NodesUtilDi {
  def n2NodesUtil: N2NodesUtil
}
