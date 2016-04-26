package models.msc.map

import com.google.inject.Inject
import io.suggest.model.geo.{GeoPoint, GeoShapeQuerable, PointGs}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.model.n2.node.{MNodeFields, MNodeTypes, MNodes}
import io.suggest.util.SioEsUtil.laFuture2sFuture
import io.suggest.ym.model.NodeGeoLevels
import models.mproj.ICommonDi
import play.api.libs.json.JsObject
import play.extras.geojson.{Feature, FeatureCollection, LatLng}

import scala.concurrent.Future


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


  def formatPoint(gp: GeoPoint): Feature[LatLng] = {
    Feature(
      geometry    = PointGs(gp).toPlayGeoJsonGeom,
      properties  = Some(JsObject(Nil))   // TODO mapbox косячит, если undefined то ошибка возникает.
    )
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
