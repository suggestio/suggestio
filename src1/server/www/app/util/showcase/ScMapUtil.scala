package util.showcase

import javax.inject.Inject
import io.suggest.es.model.IMust
import io.suggest.geo._
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNodeFields, MNodeTypes, MNodes}
import io.suggest.es.util.SioEsUtil.laFuture2sFuture
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import play.api.libs.json.JsObject
import au.id.jazzy.play.geojson.{Feature, FeatureCollection, LngLat}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 18:28
  * Description: Над-модель для работы с данными по узлам карты.
  *
  * В первой экранизации этой эпопеи была сборка просто координат узлов (ТЦ, кафе и прочее) с помощью голого _search.
  * Второй шаг был добавлением отображения георазмещений на карте.
  */
class ScMapUtil @Inject() (
  mNodes      : MNodes,
  mCommonDi   : ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi._


  /** Макс.число точек из всех adn-нод. */
  def MAX_ADN_NODES_POINTS = 2000

  /** Точность геохеша.
    * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-geohashgrid-aggregation.html#_cell_dimensions_at_the_equator]]
    */
  def ADS_AGG_GEOHASH_PRECISION = 8

  /** Макс.кол-во точек, возвращаемых из elasticsearch после geo-hash аггрегации. */
  def ADS_AGG_GEOHASHES_LIMIT = 3000


  /** Общий код поисковых полей вынесен за пределы методов. */
  protected class _MNodeSearch4Points extends MNodeSearchDfltImpl {
    override def testNode     = Some(false)
    override def isEnabled    = Some(true)
    // Смысла заваливать экран точками нет обычно. Возвращать узлы тоже смысла нет если произойдёт аггрегация.
    override def limit        = MAX_ADN_NODES_POINTS
  }

  /** Приведение одной точки к GeoJSON-представлению. */
  private def _formatPoint(gp: MGeoPoint, props: Option[JsObject]) = {
    Feature(
      geometry    = PointGsJvm.toPlayGeoJsonGeom(gp),
      properties  = props
    )
  }

  /** Кешируем результат поиска точек. */
  def getAllPoints(areaOpt: Option[IGeoShapeQuerable] = None): Future[FeatureCollection[LngLat]] = {
    cacheApiUtil.getOrElseFut("sc.map.points", expiration = 10.seconds) {
      val startedAtMs = System.currentTimeMillis()
      val adnPointsFut = findAdnPoints(areaOpt)
      val advPointsFut = findAdvPoints(areaOpt)
      for {
        adnPointsIter  <- adnPointsFut
        advsPointsIter <- advPointsFut
      } yield {
        // Объединяем все награбленные точки, попутно форматируя их в GeoJSON.
        // TODO mapbox 0.21 косячит, если undefined то ошибка возникает.
        // TODO Выставлять цвет точек в gj-пропертисы.
        val _someJsObjectEmpty = Some(JsObject(Nil))

        val allPointsFmt = (adnPointsIter ++ advsPointsIter)
          .map { gp =>
            _formatPoint(gp, _someJsObjectEmpty)
          }
          .toStream     // toSeq, но надо toImmutableSeq
        LOGGER.trace(s"getAllPoints(): Took ${System.currentTimeMillis() - startedAtMs} ms.")
        FeatureCollection(allPointsFmt)
      }
    }
  }

  /**
    * Просто вернуть точки все в рамках указанного запроса.
    *
    * @param areaOpt Область, в которой ищем точки.
    * @return Фьючерс с FeatureCollection внутри.
    */
  def findAdnPoints(areaOpt: Option[IGeoShapeQuerable] = None): Future[Iterator[MGeoPoint]] = {
    // Сборка критериев поискового запроса узлов (торг.центров, магазинов и прочих), отображаемых на карте.
    val msearch = new _MNodeSearch4Points {
      override def nodeTypes    = Seq( MNodeTypes.AdnNode )
      override def hasGeoPoint  = Some(true)

      // Эджи должны ориентироваться на предикат NodeLocation.
      override def outEdges: Seq[Criteria] = {
        // Сборка edge-критерия для выборки торговых центров и прочих объектов.
        val crBuildings = Criteria(
          predicates  = MPredicates.NodeLocation :: Nil,
          gsIntersect = Some(
            // Сборка геопоискового критерия с area или без.
            GsCriteria(
              levels = MNodeGeoLevels.NGL_BUILDING :: Nil,
              shapes = areaOpt.map(GeoShapeJvm.toEsQueryMaker).toList
            )
          ),
          // Выставляем явно should, т.к. будут ещё критерии.
          must        = IMust.SHOULD
        )

        // Вернуть итоговый список edge-критериев.
        Seq(crBuildings)
      }
    }

    // По идее требуется только значение geoPoint, весь узел считывать смысла нет.
    val fn = MNodeFields.Geo.POINT_FN
    val nodesPointsFut = mNodes.prepareSearch(msearch)
      .setFetchSource(false)
      .addDocValueField(fn) // TODO Правильно ли оно тут выставлено?
      .execute()

    for (resp <- nodesPointsFut) yield {
      lazy val logPrefix = s"findAdnPoints(${System.currentTimeMillis()}):"
      val hits = resp.getHits.getHits

      LOGGER.trace(s"$logPrefix Found ${hits.length} of total ${resp.getHits.getTotalHits} hits. Took ${resp.getTookInMillis} ms.")

      // Собрать gj-фичи
      hits.iterator
        .flatMap { hit =>
          lazy val hitInfo = s"${hit.getIndex}/${hit.getType}/${hit.getId}"
          // формат данных здесь меняется от версии к версии ES:
          // es-2.x: { "g.p": [30.23424234, -5.56756756] }
          // es-5.x: es.GeoPoint()
          val fieldValue = hit.getField(fn)
          val geoPointOpt = GeoPoint.from( fieldValue.getValue )    // Намеренно не паримся насчёт формата здесь, пусть GeoPoint разбирается
          if (geoPointOpt.isEmpty)
            LOGGER.error(s"$logPrefix Agg.values parsing failed for hit $hitInfo")
          geoPointOpt
        }
    }
  }


  /** Сборка запроса для сбора узлов, имеющих какие-то точки в adv. */
  def findAdvPoints(areaOpt: Option[IGeoShapeQuerable] = None): Future[Iterator[MGeoPoint]] = {

    // Сборка начального поиского запроса.
    val msearch = new _MNodeSearch4Points {
      override def nodeTypes = Seq( MNodeTypes.Ad )
      override def outEdges: Seq[Criteria] = {
        // 2016.sep.29: Все перечисленные предикаты имеют точки в MEdge.info.geoPoints.
        // Но выставляться эти точки начали только с сегодняшнего дня, поэтому ноды со старыми размещениями в пролёте.
        val crAdvGeo = Criteria(
          predicates = Seq(
            //MPredicates.TaggedBy.Agt,
            MPredicates.AdvGeoPlace
          ),
          // Это поле не использовалось изначально, т.к. реализация areaOpt отодвинуто на будущее.
          gsIntersect = for (area <- areaOpt) yield {
            GsCriteria(
              shapes = GeoShapeJvm.toEsQueryMaker(area) :: Nil
            )
          }
        )
        Seq( crAdvGeo )
      }
      // Будет аггрегация, сами результаты работы не важны.
      override def limit: Int = 0
    }

    // Запустить аггрегацию точек через GeoHashGrid.
    val aggEdgesName = "edges"
    val subAggGeo = "geo"

    val aggFut = mNodes.prepareSearch(msearch)
      .setSize(0)
      .addAggregation {
        AggregationBuilders.nested( aggEdgesName, MNodeFields.Edges.E_OUT_FN )
          .subAggregation {
            AggregationBuilders.geohashGrid( subAggGeo )
              .field( MNodeFields.Edges.E_OUT_INFO_GEO_POINTS_FN )
              .precision( ADS_AGG_GEOHASH_PRECISION )
              .size( ADS_AGG_GEOHASHES_LIMIT )
          }
      }
      .execute()

    lazy val logPrefix = s"geoAdvsSearch(${System.currentTimeMillis()}):"

    // Извлечь точки из ответа elasticsearch.
    for (resp <- aggFut) yield {
      val gridAgg = resp
        .getAggregations
        .get[Nested](aggEdgesName)
        .getAggregations
        .get[GeoHashGrid](subAggGeo)

      LOGGER.trace(s"$logPrefix GeoHash Grid: ${gridAgg.getBuckets.size()} buckets.")

      gridAgg
        .getBuckets
        .iterator()
        .asScala
        .flatMap { bucket =>
          val geoHash = bucket.getKeyAsString
          try {
            val gp = GeoPoint.fromGeoHash(geoHash)
            Seq( gp )
          } catch {
            case ex: Throwable =>
              LOGGER.error(s"$logPrefix Cannot parse bucket key $geoHash as geohash.", ex)
              Nil
          }
        }
    }
  }

}


/** Интерфейс для поля с DI-инстансом [[ScMapUtil]]. */
trait IScMapUtilDi {
  def scMapUtil: ScMapUtil
}
