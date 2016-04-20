package models.msc.map

import com.google.inject.Inject
import io.suggest.model.geo.{GeoPoint, GeoShapeQuerable, PointGs}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.model.n2.node.{MNodeFields, MNodeTypes, MNodes}
import io.suggest.sc.map.ScMapConstants.Nodes.Sources
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.ym.model.NodeGeoLevels
import models.mproj.ICommonDi
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid
import play.extras.geojson.{Feature, FeatureCollection, LatLng, Point}

import scala.concurrent.Future
import scala.collection.JavaConversions._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 18:28
  * Description: Над-модель для работы с данными по узлам карты.
  */
class MMapNodes @Inject() (
  mNodes      : MNodes,
  mCommonDi   : ICommonDi
) {

  import mCommonDi._

  def isClusteredZoom(zoom: Double): Boolean = {
    zoom <= 9
  }

  def MAX_POINTS = 1000

  /**
    * Сборка критериев поискового запроса узлов, отображаемых на карте.
    *
    * @param isClustered Поиск ожидается с кластеризовацией?
    * @param areaOpt Описание видимой области карты, если требуется вывести только указанную область.
    * @return Инстанс MNodeSearch.
    */
  def mapNodesQuery(isClustered: Boolean, areaOpt: Option[GeoShapeQuerable] = None): MNodeSearch = {
    new MNodeSearchDfltImpl {
      override def nodeTypes    = Seq( MNodeTypes.AdnNode )
      override def testNode     = Some(false)
      // Смысла заваливать экран точками нет обычно. Возвращать узлы тоже смысла нет если произойдёт аггрегация.
      override def limit        = if (isClustered) 0 else MAX_POINTS
      override def hasGeoPoint  = Some(true)

      // Эджи должны ориентироваться на предикат NodeLocation.
      override def outEdges: Seq[ICriteria] = {
        // Сборка геопоискового критерия.
        val gsCr = GsCriteria(
          levels = Seq( NodeGeoLevels.NGL_BUILDING ),
          shapes = areaOpt.toSeq
        )
        // Сборка edge-критерия.
        val cr = Criteria(
          predicates  = Seq( MPredicates.NodeLocation ),
          gsIntersect = Some(gsCr)
        )
        // Вернуть итоговый список edge-критериев.
        Seq(cr)
      }
    }
  }

  /** Приведение зума карты к precision.
    *
    * TODO Написать/найти формулу рассчета точности геохеша для зума mapbox карты.
    *
    * Функция будет иметь резкийграфик вида:
    * {{{
    *    ^ geohash precision
    *   8|               *
    *   7|               *
    *   6|              *
    *   5|              *
    *   4|             *
    *   3| ************
    *   2|*
    *   1|
    *   0+------------------->  mapbox zoom
    *    0 1 2 3 4 5 6 7 8 9
    * }}}
    *
    */
  def mapBoxZoom2geoHashPrecision(zoom: Double): Int = {
    if (zoom <= 0.1) {
      1
    } else if (zoom <= 0.8) {
      2
    } else if (zoom <= 7) {
      3
    } else if (zoom <= 8) {
      5
    } else {
      6
    }
  }


  /**
    * Поиск кластеров узлов под указанные критерии поиска.
    *
    * @param mapZoom Зум карты.
    * @param msearch Поисковые критерии.
    * @return Неизменяемая коллекция из GeoJSON Features, готовых к сериализации и отправки в слой карты.
    */
  def findClusteredSource(mapZoom: Double, msearch: MNodeSearch): Future[List[MNodesSource]] = {
    val aggName = "gpClust"

    val agg = AggregationBuilders.geohashGrid(aggName)
      .field( MNodeFields.Geo.POINT_FN )
      .precision( mapBoxZoom2geoHashPrecision(mapZoom) )
      .size(MAX_POINTS)

    // Требуется аггрегация, собрать agg, затолкать в запрос и исполнить.
    val reqFut = mNodes
      .dynSearchReqBuilder(msearch)
      .addAggregation(agg)
      .execute()

    // Результаты отмаппить как GeoJSON точки с данными в props для рендера круговых маркетов.
    for (resp <- reqFut) yield {
      val buckets = resp.getAggregations
        .get[GeoHashGrid](aggName)
        .getBuckets

      // Собрать данные кластеров
      val srcs0 = {
        val clustersIter = buckets.iterator()
          .filter(_.getDocCount > 1L)
        if (clustersIter.nonEmpty) {
          val gjClusters = clustersIter
            .map(formatCluster)
            .toStream
          val src = MNodesSource(
            srcName   = Sources.CLUSTERS,
            clustered = true,
            features  = FeatureCollection(gjClusters)
          )
          List(src)
        } else {
          // Нет кластеров
          Nil
        }
      }

      // Скомпилить одноточечные кластеры в точки
      val srcs1 = {
        val pointsIter = buckets.iterator()
          .filter(_.getDocCount == 1L)
        if (pointsIter.nonEmpty) {
          val pts = pointsIter
            .map { b =>
              Feature(
                geometry = formatEsPointGeom(b)
              )
            }
            .toStream
          val src = MNodesSource(
            srcName = Sources.POINTS,
            clustered = false,
            features = FeatureCollection(pts)
          )
          src :: srcs0

        } else {
          srcs0
        }
      }

      // Вернуть список итоговых сорсов
      srcs1
    }
  }


  def formatEsPointGeom(b: GeoHashGrid.Bucket): Point[LatLng] = {
    formatEsPointGeom(b.getKeyAsGeoPoint)
  }
  def formatEsPointGeom(p: org.elasticsearch.common.geo.GeoPoint): Point[LatLng] = {
    val gp = GeoPoint(p)
    PointGs(gp).toPlayGeoJsonGeom
  }

  /** Заворачивание одного багета кластера в GeoJSON Feature. */
  def formatCluster(b: GeoHashGrid.Bucket): Feature[LatLng] = {
    val mProps = MNodesClusterProps(b.getDocCount)
    Feature(
      properties  = Some( MNodesClusterProps.FORMAT.writes(mProps) ),
      geometry    = formatEsPointGeom(b)
    )
  }

  def formatPoint(gp: GeoPoint): Feature[LatLng] = {
    Feature(
      geometry    = PointGs(gp).toPlayGeoJsonGeom
    )
  }


  /**
    * Заворачивание отрендеренных точек в data-контейнеры данных для слоёв.
    *
    * @param featsColl GeoJSON FeatureCollection.
    * @return Опциональный MNodesSource.
    */
  def points2NodeSources(featsColl: FeatureCollection[LatLng]): Option[MNodesSource] = {
    if (featsColl.features.nonEmpty) {
      // Собрать описание сорса
      val src = MNodesSource(
        srcName   = Sources.POINTS,
        clustered = false,
        features  = featsColl
      )
      // Вернуть источники.
      Some(src)

    } else {
      None
    }
  }

  /**
    * Просто вернуть точки все в рамках указанного запроса.
    *
    * @param msearch Поисковый запрос точек.
    * @return Фьючерс с FeatureCollection внутри.
    */
  def getPoints(msearch: MNodeSearch): Future[FeatureCollection[LatLng]] = {
    // По идее требуется только значение geoPoint, весь узел считывать смысла нет.
    val fn = MNodeFields.Geo.POINT_FN
    mNodes.dynSearchReqBuilder(msearch)
      .setFetchSource(false)
      .addFields(fn)
      .execute()
      .map { resp =>
        // Собрать gj-фичи
        val feats = resp.getHits
          .getHits
          .iterator
          .flatMap { h =>
            // формат данных здесь примерно такой: { "g.p": [30.23424234, -5.56756756] }
            val lonLatArr = h.field(fn).getValues
            GeoPoint.deserializeOpt(lonLatArr)
          }
          .map(formatPoint)
          .toStream

        FeatureCollection(feats)
      }
  }

}

/** Интерфейс для поля с DI-инстансом [[MMapNodes]]. */
trait IMMapNodesDi {
  def mMapNodes: MMapNodes
}
