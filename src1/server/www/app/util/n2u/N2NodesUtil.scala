package util.n2u

import io.suggest.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.extra.domain.MDomainModes
import io.suggest.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.15 15:44
  * Description: Утиль для узлов N2.
  */
final class N2NodesUtil {

  /** Попытаться узлать продьюсера узла. */
  def madProducerId(mad: MNode): Option[String] = {
    mad.edges
      .withPredicateIterIds( MPredicates.OwnedBy )
      .nextOption()
  }

  /**
    * Выдать эджи карточки (узла), отфитльтровав всех внешних ресиверов.
    * @param mad Исходная рекламная карточка.
    * @return Итератор эджей, содержащий исходные эджи включая локального ресивера, если таковой был.
    */
  def withoutExtReceivers(mad: MNode): Iterator[MEdge] = {
    val producerIdOpt = madProducerId(mad)
    mad.edges.out.iterator.filter { e =>
      e.predicate.eqOrHasParent(MPredicates.Receiver) || producerIdOpt.exists(e.nodeIds.contains)
    }
  }

  /** Попытаться узлать ресиверов узла. */
  def receiverIds(mad: MNode): Iterator[String] = {
    mad.edges
      .withPredicateIterIds( MPredicates.Receiver )
  }

  /** Попытаться узнать ресиверов в поисковых критериях эджей. */
  def receiverIds(crs: IterableOnce[Criteria]): Iterator[String] = {
    crs
      .iterator
      .filter { _.predicates.exists(_ eqOrHasParent MPredicates.Receiver) }
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
      .filter( _.info.flag contains false )
  }

  /** Найти указанный ресивер среди эджей. */
  def findReceiver(mad: MNode, nodeId: String): Option[MEdge] = {
    // Тот редкий случай, когда в карте эджей можно поискать по ключу.
    mad.edges
      .withNodePred(nodeId, MPredicates.Receiver)
      .nextOption()
  }

  /**
    * Бывает, что необходимо сгенерить префикс ссылки на внешний сайт,
    * возможно привязанный к указанному узлу.
    * @param mnode Узел N2.
    * @return Опциональный результат работы вида: http://site.com
    */
  def urlPrefixOf(mnode: MNode): Iterator[String] = {
    mnode.extras.domains
      .iterator
      .filter(_.mode == MDomainModes.ScServeIncomingRequests)
      .map { mdx =>
        mdx.proto + "://" + mdx.dkey
      }
  }

}


/** Интерфейс доступа к DI-полю. */
trait IN2NodesUtilDi {
  def n2NodesUtil: N2NodesUtil
}
