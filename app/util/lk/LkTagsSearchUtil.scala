package util.lk

import javax.inject.Inject

import com.google.inject.Singleton
import io.suggest.common.tags.TagFacesUtil
import io.suggest.common.tags.search.{MTagFound, MTagsFound}
import io.suggest.model.es.IEsModelDiVal
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, TagCriteria}
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import models.mlk.MLkTagsSearchQs

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 16:15
  * Description: Утиль для взаимодействия с тегами, живующими внутри эджей.
  */
@Singleton
class LkTagsSearchUtil @Inject() (
  mNodes    : MNodes,
  mCommonDi : IEsModelDiVal
) {

  import mCommonDi._


  /** Дефолтовое значение limit, если не указано или некорректно. */
  private def LIMIT_DFLT    = 10

 /** Компиляция qs в аргументы поиска узлов. */
  def qs2TagNodesSearch(qs: MLkTagsSearchQs): Future[MNodeSearch] = {

    val _limit = qs.limitOpt
      .getOrElse( LIMIT_DFLT )

    val _offset = qs.offsetOpt
      .getOrElse( 0 )

    val tags: Seq[String] = TagFacesUtil.query2tags( qs.tagsQuery )
    val searchTagOpt  = tags.lastOption

    val tcrOpt = for (q <- searchTagOpt) yield {
      TagCriteria(q, isPrefix = true)
    }
    val _edgeSearchCr = Criteria(
      predicates  = Seq( MPredicates.TaggedBy.Self ),
      tags        = tcrOpt.toSeq
    )

    val r = new MNodeSearchDfltImpl {
      override def outEdges  = Seq(_edgeSearchCr)
      override def limit     = _limit
      override def offset    = _offset
      override def nodeTypes = Seq( MNodeTypes.Tag )
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
  def liveSearchTagsFromQs(qs: MLkTagsSearchQs): Future[MTagsFound] = {
    qs2TagNodesSearch(qs)
      .flatMap(tagsSearchHint)
  }

}


trait ITagSearchUtilDi {
  def tagSearchUtil: LkTagsSearchUtil
}
