package models.msc.map

import com.google.inject.Inject
import io.suggest.model.geo.{GeoPoint, PointGs}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.model.n2.node.{MNodeFields, MNodeTypes, MNodes}
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.ym.model.NodeGeoLevels
import models.mproj.ICommonDi
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid
import play.extras.geojson.{Feature, LatLng}

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
    zoom >= 9
  }

  /**
    * Сборка критериев поискового запроса узлов, отображаемых на карте.
    *
    * @param isClustered Поиск ожидается с кластеризовацией?
    * @param mapInfo Описание видимой области карты.
    * @return Инстанс MNodeSearch.
    */
  def mapNodesQuery(isClustered: Boolean, mapInfo: MMapAreaInfo): MNodeSearch = {
    new MNodeSearchDfltImpl {
      override def nodeTypes    = Seq( MNodeTypes.AdnNode )
      override def testNode     = Some(false)
      // Смысла заваливать экран точками нет обычно. Возвращать узлы тоже смысла нет если произойдёт аггрегация.
      override def limit        = if (isClustered) 0 else 100
      override def hasGeoPoint  = Some(true)

      // Эджи должны ориентироваться на предикат NodeLocation.
      override def outEdges: Seq[ICriteria] = {
        // Сборка геопоискового критерия.
        val gsCr = GsCriteria(
          levels = Seq( NodeGeoLevels.NGL_BUILDING ),
          shapes = Seq( mapInfo.envelope )
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


  /**
    * Поиск кластеров узлов под указанные критерии поиска.
    * @param msearch Поисковые критерии.
    * @return Неизменяемая коллекция из GeoJSON Features, готовых к сериализации и отправки в слой карты.
    */
  def findClusters(msearch: MNodeSearch): Future[Stream[Feature[LatLng]]] = {
    val aggName = "gpClust"
    val agg = AggregationBuilders.geohashGrid(aggName)
      .precision(6)
      .size(20)

    // Требуется аггрегация, собрать agg, затолкать в запрос и исполнить.
    val reqFut = mNodes
      .dynSearchReqBuilder(msearch)
      .addAggregation(agg)
      .execute()

    // Результаты отмаппить как GeoJSON точки с данными в props для рендера круговых маркетов.
    for (resp <- reqFut) yield {
      resp.getAggregations
        .get[GeoHashGrid](aggName)
        .getBuckets
        .iterator()
        .toIterator
        // Завернуть каждый багет в GeoJSON Feature.
        .map { b =>
          val gp = GeoPoint( b.getKeyAsGeoPoint )
          val mProps = MNodesClusterProps(b.getDocCount)
          Feature(
            properties  = Some( MNodesClusterProps.FORMAT.writes(mProps) ),
            geometry    = PointGs(gp).toPlayGeoJsonGeom
          )
        }
        .toStream
    }
  }


  /**
    * Поиск точек узлов для непосредственного отображения оных.
    * @param msearch Критерии поиска точек.
    * @return Фьючерс с потоком GeoJSON Features.
    */
  def findNodePoints(msearch: MNodeSearch): Future[Stream[Feature[LatLng]]] = {
    // По идее требуется только значение geoPoint, весь узел считывать смысла нет.
    val fn = MNodeFields.Geo.POINT_FN
    mNodes.dynSearchReqBuilder(msearch)
      .setFetchSource(false)
      .addFields(fn)
      .execute()
      .map { resp =>
        resp.getHits
          .getHits
          .iterator
          .flatMap(_.field(fn).getValues)
        ???
      }
  }

}

/** Интерфейс для поля с DI-инстансом [[MMapNodes]]. */
trait IMMapNodesDi {
  def mMapNodes: MMapNodes
}
