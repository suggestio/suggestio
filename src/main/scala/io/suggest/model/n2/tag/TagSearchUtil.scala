package io.suggest.model.n2.tag

import javax.inject.Inject

import com.google.inject.Singleton
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.model.n2.node.{MNode, MNodeFields}
import io.suggest.util.SioEsUtil.laFuture2sFuture
import org.elasticsearch.client.Client
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import scala.collection.JavaConversions._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 16:15
  * Description: Утиль для взаимодействия с тегами, живующими внутри эджей.
  */
@Singleton
class TagSearchUtil @Inject() (implicit ec: ExecutionContext, client: Client) {

  /**
    * Поиск тегов по имени.
    *
    * @param nodeSearch Критерии поиска тегов.
    * @return
    */
  def liveSearchTagByName(nodeSearch: MNodeSearch): Future[TagsSearchResult] = {
    for (found <- MNode.dynSearch(nodeSearch)) yield {
      val infos = for {
        tn      <- found
        tagEdge <- tn.edges
          .withPredicateIter( MPredicates.TaggedBy.Self )
          .toSeq
        tFace   <- tagEdge.info.tags.headOption
        tCount  <- tagEdge.order
      } yield {
        TagFoundInfo(
          face  = tFace,
          count = tCount
        )
      }

      TagsSearchResult(
        tags = infos
      )
    }
  }

}

trait ITagSearchUtilDi {
  def tagSearchUtil: TagSearchUtil
}
