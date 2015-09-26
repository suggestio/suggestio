package models.adv.search

import models.MPredicates
import io.suggest.model.n2.edge.search.EdgeSearchDfltImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 23:58
 * Description: Реализация аргументов поиска N2-рёбер делегирования права обработки
 * входящих запросов размещения.
 *
 * Т.е. если есть узел, то ему могут быть делегированы права обработки запросов размещения от других узлов.
 * Тогда, если этот узел хочет узнать id узлов, которые делегировали ему это право, то этот узел
 * должен вставить свой id в этот запрос и исполнить его.
 */
case class FindEdgesByAdvDelegate(
  nodeId                : String,
  override val limit    : Int = 100
)
  extends EdgeSearchDfltImpl
{

  override def predicates = Seq( MPredicates.AdvManageDelegatedTo )

  override def toId       = Seq( nodeId )

}
