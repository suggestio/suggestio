package models.im.logo

import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.EdgeSearchDfltImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 18:50
 * Description: Поисковые аргументы для поиска в MEdge намёков на логотипы.
 */
object LogoEdgesSearch {

  def apply(nodeId: String): LogoEdgesSearch = {
    apply( Seq(nodeId) )
  }

}


case class LogoEdgesSearch(
  override val fromId: Seq[String]
)
  extends EdgeSearchDfltImpl
{

  override def predicates = Seq( MPredicates.Logo )
  override def limit      = fromId.size

}
