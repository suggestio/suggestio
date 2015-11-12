package util.n2u

import com.google.inject.Singleton
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.ICriteria
import models.{BlockConf, MEdge, MNode}
import util.blocks.BlocksConf

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.15 15:44
  * Description: Утиль для узлов N2.
  */
@Singleton
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

  /** Найти первый (любой) отказ от  */
  def mdrs(mad: MNode): Iterator[MEdge] = {
    mad.edges
      .withPredicateIter(MPredicates.ModeratedBy)
      .filter( _.info.flag.contains(false) )
  }

  /** Найти указанный ресивер среди эджей. */
  def findReceiver(mad: MNode, nodeId: String): Option[MEdge] = {
    // Тот редкий случай, когда в карте эджей можно поискать по ключу.
    mad.edges.out
      .get( (MPredicates.Receiver, nodeId) )
  }

  /** Определить BlockConf для карточки или иного узла. */
  def bc(mad: MNode): BlockConf = {
    val blockIdOpt = {
      mad.ad
        .blockMeta
        .map(_.blockId)
    }
    BlocksConf.applyOrDefault(blockIdOpt)
  }

}


/** Интерфейс доступа к DI-полю. */
trait IN2NodesUtilDi {
  def n2NodesUtil: N2NodesUtil
}
