package util.lk

import javax.inject.Inject
import javax.inject.Singleton
import io.suggest.common.tags.TagFacesUtil
import io.suggest.common.tags.search.{MTagFound, MTagsFound}
import io.suggest.es.model.EsModel
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.{Criteria, TagCriteria}
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.tags.MTagsSearchQs

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 16:15
  * Description: Утиль для взаимодействия с тегами, живующими внутри эджей.
  */
@Singleton
class LkTagsSearchUtil @Inject() (
                                   esModel                    : EsModel,
                                   mNodes                     : MNodes,
                                   implicit private val ec    : ExecutionContext,
                                 ) {

  import esModel.api._


  /** Дефолтовое значение limit, если не указано или некорректно. */
  private def LIMIT_DFLT    = 10

 /** Компиляция qs в аргументы поиска узлов. */
  def qs2TagNodesSearch(qs: MTagsSearchQs): Future[MNodeSearch] = {

    val _limit = qs.limit
      .getOrElse( LIMIT_DFLT )

    val _offset = qs.offset
      .getOrElse( 0 )

    val tags: Seq[String] = TagFacesUtil.query2tags( qs.faceFts )
    val searchTagOpt  = tags.lastOption

    val tcrOpt = for (q <- searchTagOpt) yield {
      TagCriteria(q, isPrefix = true)
    }
    val _edgeSearchCr = Criteria(
      predicates  = MPredicates.TaggedBy.Self :: Nil,
      tags        = tcrOpt.toSeq
    )

    val r = new MNodeSearch {
      override def outEdges  = _edgeSearchCr :: Nil
      override def limit     = _limit
      override def offset    = _offset
      override def nodeTypes = MNodeTypes.Tag :: Nil
    }

    Future.successful(r)
  }


  /**
    * Поиск тегов по имени.
    *
    * @param nodeSearch Критерии поиска тегов.
    * @return
    */
  def tagsSearchHint(nodeSearch: MNodeSearch): Future[MTagsFound] = {
    for {
      found <- mNodes.dynSearch(nodeSearch)
    } yield {
      val infos = for {
        tn      <- found
        tagEdge <- tn.edges
          .withPredicateIter( MPredicates.TaggedBy.Self )
          .toSeq
        tFace   <- tagEdge.info.tags.headOption
        tCount  <- tagEdge.order
      } yield {
        MTagFound(
          face  = tFace,
          count = tCount
        )
      }

      MTagsFound(
        tags = infos
      )
    }
  }


  /** Комбо из liveSearchTags() и qs2TagNodesSearch(). */
  def liveSearchTagsFromQs(qs: MTagsSearchQs): Future[MTagsFound] = {
    qs2TagNodesSearch(qs)
      .flatMap(tagsSearchHint)
  }

}


trait ITagSearchUtilDi {
  def tagSearchUtil: LkTagsSearchUtil
}
