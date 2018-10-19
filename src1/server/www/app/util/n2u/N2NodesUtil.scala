package util.n2u

import javax.inject.Singleton
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.extra.domain.MDomainModes
import io.suggest.model.n2.node.MNode

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

  /**
    * Выдать эджи карточки (узла), отфитльтровав всех внешних ресиверов.
    * @param mad Исходная рекламная карточка.
    * @return Итератор эджей, содержащий исходные эджи включая локального ресивера, если таковой был.
    */
  def withoutExtReceivers(mad: MNode): Iterator[MEdge] = {
    val producerIdOpt = madProducerId(mad)
    mad.edges.iterator.filter { e =>
      e.predicate.eqOrHasParent(MPredicates.Receiver) || producerIdOpt.exists(e.nodeIds.contains)
    }
  }

  /** Попытаться узлать ресиверов узла. */
  def receiverIds(mad: MNode): Iterator[String] = {
    mad.edges
      .withPredicateIterIds( MPredicates.Receiver )
  }

  /** Попытаться узнать ресиверов в поисковых критериях эджей. */
  def receiverIds(crs: TraversableOnce[Criteria]): Iterator[String] = {
    crs.toIterator
      .filter { _.containsPredicate(MPredicates.Receiver) }
      .flatMap( _.nodeIds )
  }

  def receivers(mad: MNode): Iterator[MEdge] = {
    mad.edges
      .withPredicateIter( MPredicates.Receiver )
  }

  /** Собрать карту ресиверов. */
  def receiversMap(mad: MNode): Seq[MEdge] = {
    MNodeEdges.edgesToMap1( receivers(mad) )
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
    mad.edges
      .withNodePred(nodeId, MPredicates.Receiver)
      .toStream
      .headOption
  }

  /**
    * Бывает, что необходимо сгенерить префикс ссылки на внешний сайт,
    * возможно привязанный к указанному узлу.
    * @param mnode Узел N2.
    * @return Опциональный результат работы вида: http://site.com
    */
  def urlPrefixOf(mnode: MNode): Option[String] = {
    mnode.extras.domains
      .find(_.mode == MDomainModes.ScServeIncomingRequests)
      .map { mdx =>
        mdx.proto + "://" + mdx.dkey
      }
  }

}


/** Интерфейс доступа к DI-полю. */
trait IN2NodesUtilDi {
  def n2NodesUtil: N2NodesUtil
}
