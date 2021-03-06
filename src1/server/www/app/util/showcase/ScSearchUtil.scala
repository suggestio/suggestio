package util.showcase

import io.suggest.common.empty.OptionUtil
import io.suggest.common.tags.TagFacesUtil
import io.suggest.es.model.{IMust, MEsNestedSearch}
import io.suggest.geo.{CircleGs, GeoShapeToEsQuery, MGeoLoc, MNodeGeoLevels}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.{Criteria, GsCriteria, TagCriteria}
import io.suggest.n2.node.{MNodeType, MNodeTypes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.sc.sc3.MScQs

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.16 15:51
  * Description: Утиль для поиска тегов в выдаче.
  */
final class ScSearchUtil {

  /** Дефолтовое значение limit, если не указано или некорректно. */
  private def LIMIT_DFLT    = 10

  private def FTS_SEARCH_RADIUS_M = 100000

  /** Список разрешённых для поиска типов узлов. */
  private def _nodeTypes4search: List[MNodeType] =
    MNodeTypes.Tag ::
    MNodeTypes.AdnNode ::
    Nil


  /** Компиляция значений query string в MNodeSearch. */
  def qs2NodesSearch(qs: MScQs, qsGeoLocs2: Seq[MGeoLoc], radioCtx: MRadioBeaconsSearchCtx): Future[MNodeSearch] = {
    // По каким типа узлов фильтровать? Зависит от текущей вкладки поиска.
    //val tabOpt = qs.search.tab
    //val isMultiSearch = tabOpt.isEmpty

    //val isSearchTags = isMultiSearch || tabOpt.contains(MSearchTabs.Tags)
    //val isSearchGeoRcvrs = (isMultiSearch || tabOpt.contains(MSearchTabs.GeoMap)) && geoLocOpt2.nonEmpty
    // По какоми эджам орудовать?
    var edgesCrs = List.empty[Criteria]

    val must = IMust.MUST
    val should = IMust.SHOULD

    val queryTextTags: Seq[String] = TagFacesUtil.queryOpt2tags( qs.search.textQuery )

    val tagCrs: Seq[TagCriteria] = if (queryTextTags.isEmpty) {
      Nil
    } else {
      val tagsSet = queryTextTags.toSet
      val lastTagOpt  = queryTextTags.lastOption
      tagsSet
        .iterator
        .map { tagFace =>
          TagCriteria(
            face      = tagFace,
            isPrefix  = lastTagOpt contains tagFace,
            must      = must
          )
        }
        .toSeq
    }

    val distanceSort = qsGeoLocs2
      .headOption
      .map(_.point)

    // LocEnv tags from radio-beacons: Also do tags search in all visible beacons:
    if (radioCtx.uidsClear.nonEmpty) {
      edgesCrs ::= Criteria(
        predicates  = MPredicates.TaggedBy.Self :: Nil,
        tags        = tagCrs,
        nodeIds     = radioCtx.uidsClearSeq,
        must        = should,
      )
    }

    // Поиск по тегам.
    if (qsGeoLocs2.nonEmpty) {
      edgesCrs ::= Criteria(
        predicates  = MPredicates.TaggedBy.Self :: Nil,
        tags        = tagCrs,
        nodeIds     = qs.search.rcvrId.toStringOpt.toSeq,
        // Отработать геолокацию: искать только теги, размещенные в текущей области (но если НЕ задан id узла-ресивера)
        gsIntersect = Option.when( qs.search.rcvrId.isEmpty ) {
          GsCriteria(
            levels = MNodeGeoLevels.geoTag :: Nil,
            shapes = for (geoLoc <- qsGeoLocs2) yield {
              val circle = CircleGs(
                center  = geoLoc.point,
                radiusM = 1
              )
              GeoShapeToEsQuery( circle )
            },
          )
        },
        must = should,
        geoDistanceSort = distanceSort
      )

      // Search for geo-nodes by NodeLocation.
      if (tagCrs.nonEmpty) {
        // TODO Поиск по названию, с названием или даже без, с учётом координат.
        edgesCrs ::= Criteria(
          predicates  = MPredicates.NodeLocation :: Nil,
          // Ограничить поиск радиусом от текущей точки. Она обязательно задана, иначе бы этот код не вызывался (см. флаг выше).
          gsIntersect = Some {
            GsCriteria(
              levels = MNodeGeoLevels.geoPlacesSearchAt,
              shapes = for (geoLoc <- qsGeoLocs2) yield {
                val circle = CircleGs(
                  center  = geoLoc.point,
                  // 100км вокруг текущей точки
                  radiusM = FTS_SEARCH_RADIUS_M
                )
                GeoShapeToEsQuery( circle )
              },
            )
          },
          must        = should,
          // Поиск по имени проходит через индекс тегов, куда должно быть сохранено имя в соотв. adv-билдере
          tags        = tagCrs,
          geoDistanceSort = distanceSort
        )
      }
    }

    // TODO with distance sort - актуально для списка результатов. Менее актуально для карты (но всё-таки тоже актуально).

    // Собрать итоговый поиск.
    val r = new MNodeSearch {
      override val outEdges: MEsNestedSearch[Criteria] =
        MEsNestedSearch.plain( edgesCrs: _* )
      override val limit = qs.search.limit getOrElse LIMIT_DFLT
      override val offset = qs.search.offset getOrElse 0
      override val nodeTypes = _nodeTypes4search
      override def isEnabled = OptionUtil.SomeBool.someTrue
    }

    Future.successful(r)
  }

}
