package io.suggest.model.n2.tag

import javax.inject.Inject

import com.google.inject.Singleton
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.MNodes
import io.suggest.model.n2.node.search.MNodeSearch
import org.elasticsearch.client.Client

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 16:15
  * Description: Утиль для взаимодействия с тегами, живующими внутри эджей.
  */
@Singleton
class TagSearchUtil @Inject() (mNodes: MNodes, implicit val ec: ExecutionContext, implicit val client: Client) {

  /**
    * Поиск тегов по имени.
    *
    * @param nodeSearch Критерии поиска тегов.
    * @return
    */
  def liveSearchTagByName(nodeSearch: MNodeSearch): Future[TagsSearchResult] = {
    for (found <- mNodes.dynSearch(nodeSearch)) yield {
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
