package util.showcase

import javax.inject.{Inject, Singleton}
import io.suggest.common.tags.TagFacesUtil
import io.suggest.es.model.IMust
import io.suggest.geo.{CircleGs, CircleGsJvm, MGeoLoc, MNodeGeoLevels}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, TagCriteria}
import io.suggest.model.n2.node.{MNodeType, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.sc.sc3.MScQs
import io.suggest.sc.search.MSearchTabs

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.16 15:51
  * Description: Утиль для поиска тегов в выдаче.
  */
@Singleton
class ScSearchUtil @Inject()(
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

    // По каким типа узлов фильтровать? Зависит от текущей вкладки поиска.
    var nodeTypes4search = List.empty[MNodeType]
    val tabOpt = qs.search.tab
    val isMultiSearch = tabOpt.isEmpty

    val isSearchTags = isMultiSearch || tabOpt.contains(MSearchTabs.Tags)
    if (isSearchTags)
      nodeTypes4search ::= MNodeTypes.Tag

    val isSearchGeoRcvrs = (isMultiSearch || tabOpt.contains(MSearchTabs.GeoMap)) && geoLocOpt2.nonEmpty
    if (isSearchGeoRcvrs)
      nodeTypes4search ::= MNodeTypes.AdnNode

    // По какоми эджам орудовать?
    var edgesCrs = List.empty[Criteria]
    val should = IMust.SHOULD

    // Собрать поиск по тегам:
    if (isSearchTags) {
      val tags: Seq[String] = TagFacesUtil.queryOpt2tags( qs.search.textQuery )
      // TODO Надо отрабатывать все части, а не только последнюю
      val searchTagOpt  = tags.lastOption

      val tcrOpt = for (q <- searchTagOpt) yield {
        TagCriteria(
          face      = q,
          isPrefix  = true
        )
      }

      edgesCrs ::= Criteria(
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
        },
        must = should
      )
    }

    // Активировать поиск по ресиверам.
    if (isSearchGeoRcvrs) {
      // TODO Поиск по названию, с названием или даже без, с учётом координат.
      edgesCrs ::= Criteria(
        predicates  = MPredicates.NodeLocation :: Nil,
        must        = should,
        // Ограничить поиск радиусом от текущей точки. Она обязательно задана, иначе бы этот код не вызывался (см. флаг выше).
        gsIntersect = for (geoLoc <- geoLocOpt2) yield {
          val circle = CircleGs(
            center  = geoLoc.point,
            // 100км вокруг текущей точки
            radiusM = 100000
          )
          GsCriteria(
            levels = MNodeGeoLevels.geoPlacesSearchAt,
            shapes = CircleGsJvm.toEsQueryMaker(circle) :: Nil
          )
        }
      )

      // TODO with distance sort
      // TODO name search
    }

    // Собрать итоговый поиск.
    val r = new MNodeSearchDfltImpl {
      override def outEdges  = edgesCrs
      override def limit     = _limit
      override def offset    = _offset
      override def nodeTypes = nodeTypes4search
    }

    Future.successful(r)
  }

}

/** Интерфейс для DI-поля с инстансом [[ScSearchUtil]]. */
trait IScTagsUtilDi {
  def scTagsUtil: ScSearchUtil
}