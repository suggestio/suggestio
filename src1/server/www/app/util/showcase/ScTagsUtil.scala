package util.showcase

import javax.inject.{Inject, Singleton}
import io.suggest.common.tags.TagFacesUtil
import io.suggest.geo.{CircleGs, CircleGsJvm, MGeoLoc, MNodeGeoLevels}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, TagCriteria}
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.sc.sc3.MScQs

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.16 15:51
  * Description: Утиль для поиска тегов в выдаче.
  */
@Singleton
class ScTagsUtil @Inject()(
  mNodes    : MNodes
) {

  /** Дефолтовое значение limit, если не указано или некорректно. */
  private def LIMIT_DFLT    = 10


  /** Компиляция значений query string в MNodeSearch. */
  def qs2NodesSearch(qs: MScQs): Future[MNodeSearch] = {
    qs2NodesSearch(qs, qs.common.locEnv.geoLocOpt)
  }
  def qs2NodesSearch(qs: MScQs, geoLocOpt2: Option[MGeoLoc]): Future[MNodeSearch] = {

    val _limit = qs.search.limit
      .getOrElse( LIMIT_DFLT )

    val _offset = qs.search.offset
      .getOrElse( 0 )

    val tags: Seq[String] = TagFacesUtil.queryOpt2tags( qs.search.textQuery )
    val searchTagOpt  = tags.lastOption

    val tcrOpt = for (q <- searchTagOpt) yield {
      TagCriteria(
        face      = q,
        isPrefix  = true
      )
    }

    val _edgeSearchCr = Criteria(
      predicates  = MPredicates.TaggedBy.Self :: Nil,
      tags        = tcrOpt.toSeq,
      nodeIds     = qs.search.rcvrId.toStringOpt.toSeq,
      // Отработать геолокацию: искать только теги, размещенные в текущей области.
      gsIntersect = for (geoLoc <- geoLocOpt2) yield {
        val circle = CircleGs(
          center  = geoLoc.point,
          radiusM = 1
        )
        GsCriteria(
          levels = MNodeGeoLevels.geoTag :: Nil,
          shapes = CircleGsJvm.toEsQueryMaker(circle) :: Nil
        )
      }
    )

    val r = new MNodeSearchDfltImpl {
      override def outEdges  = _edgeSearchCr :: Nil
      override def limit     = _limit
      override def offset    = _offset
      override def nodeTypes = MNodeTypes.Tag :: Nil
    }

    Future.successful(r)
  }

}

/** Интерфейс для DI-поля с инстансом [[ScTagsUtil]]. */
trait IScTagsUtilDi {
  def scTagsUtil: ScTagsUtil
}
