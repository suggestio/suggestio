package util.showcase

import com.google.inject.{Inject, Singleton}
import io.suggest.common.tags.TagFacesUtil
import io.suggest.model.geo.{CircleGs, Distance}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, TagCriteria}
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.ym.model.NodeGeoLevels
import models.mgeo.MGeoLoc
import models.msc.tag.MScTagsSearchQs
import org.elasticsearch.common.unit.DistanceUnit

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
  def qs2NodesSearch(qs: MScTagsSearchQs): Future[MNodeSearch] = {
    qs2NodesSearch(qs, qs.locEnv.geoLocOpt)
  }
  def qs2NodesSearch(qs: MScTagsSearchQs, geoLocOpt2: Option[MGeoLoc]): Future[MNodeSearch] = {

    val _limit = qs.limitOpt
      .getOrElse( LIMIT_DFLT )

    val _offset = qs.offsetOpt
      .getOrElse( 0 )

    val tags: Seq[String] = TagFacesUtil.queryOpt2tags( qs.tagsQuery )
    val searchTagOpt  = tags.lastOption

    val tcrOpt = for (q <- searchTagOpt) yield {
      TagCriteria(
        face      = q,
        isPrefix  = true
      )
    }

    val _edgeSearchCr = Criteria(
      predicates  = Seq( MPredicates.TaggedBy.Self ),
      tags        = tcrOpt.toSeq,
      // Отработать геолокацию: искать только теги, размещенные в текущей области.
      gsIntersect = for (geoLoc <- geoLocOpt2) yield {
        GsCriteria(
          levels = Seq( NodeGeoLevels.geoTag ),
          shapes = Seq(
            CircleGs(
              center = geoLoc.center,
              radius = Distance(1, DistanceUnit.METERS)
            )
          )
        )
      }
    )

    val r = new MNodeSearchDfltImpl {
      override def outEdges  = Seq(_edgeSearchCr)
      override def limit     = _limit
      override def offset    = _offset
      override def nodeTypes = Seq( MNodeTypes.Tag )
    }

    Future.successful(r)
  }

}

/** Интерфейс для DI-поля с инстансом [[ScTagsUtil]]. */
trait IScTagsUtilDi {
  def scTagsUtil: ScTagsUtil
}
