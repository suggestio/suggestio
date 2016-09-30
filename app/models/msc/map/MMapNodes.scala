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
import play.api.libs.json.{JsArray, JsNumber, JsObject}
import play.extras.geojson.{Feature, FeatureCollection, LatLng}
import util.PlayMacroLogsImpl

import scala.collection.JavaConversions._
import scala.concurrent.Future


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 18:28
  * Description: Над-модель для работы с данными по узлам карты.
  *
  * В первой экранизации этой эпопеи была сборка просто координат узлов (ТЦ, кафе и прочее) с помощью голого _search.
  * Второй шаг был добавлением отображения георазмещений на карте.
  */
class MMapNodes @Inject() (
  mNodes      : MNodes,
  mCommonDi   : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._

  def MAX_POINTS = 2000

  /** Общий код поисковых полей вынесен за пределы методов. */
  protected class _MNodeSearch4Points extends MNodeSearchDfltImpl {
    override def testNode     = Some(false)
    override def isEnabled    = Some(true)
    // Смысла заваливать экран точками нет обычно. Возвращать узлы тоже смысла нет если произойдёт аггрегация.
    override def limit        = MAX_POINTS
  }

  /**
    * Сборка критериев поискового запроса узлов (торг.центров, магазинов и прочих), отображаемых на карте.
    *
    * @param areaOpt Описание видимой области карты, если требуется вывести только указанную область.
    * @return Инстанс MNodeSearch.
    */
  def buildingsQuery(areaOpt: Option[GeoShapeQuerable] = None): MNodeSearch = {
    new _MNodeSearch4Points {
      override def nodeTypes    = Seq( MNodeTypes.AdnNode )
      override def hasGeoPoint  = Some(true)

      // Эджи должны ориентироваться на предикат NodeLocation.
      override def outEdges: Seq[ICriteria] = {
        // Сборка edge-критерия для выборки торговых центров и прочих объектов.
        val crBuildings = Criteria(
          predicates  = Seq( MPredicates.NodeLocation ),
          gsIntersect = Some(
            // Сборка геопоискового критерия с area или без.
            GsCriteria(
              levels = Seq( NodeGeoLevels.NGL_BUILDING ),
              shapes = areaOpt.toSeq
            )
          ),
          // Выставляем явно should, т.к. будут ещё критерии.
          must        = None
        )

        // Вернуть итоговый список edge-критериев.
        Seq(crBuildings)
      }
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
        lazy val logPrefix = s"getPoints(${System.currentTimeMillis()}):"
        LOGGER.trace(s"$logPrefix Found ES-search ${resp.getTookInMillis}")
        // Собрать gj-фичи
        val feats = resp.getHits
          .getHits
          .iterator
          .flatMap { hit =>
            lazy val hitInfo = s"${hit.getIndex}/${hit.getType}/${hit.getId}"
            // формат данных здесь примерно такой: { "g.p": [30.23424234, -5.56756756] }
            val fieldValue = hit.field(fn)
            val lonLatArr = fieldValue
              .getValues
              .iterator()
              .flatMap {
                case v: java.lang.Number =>
                  val n = JsNumber( v.doubleValue() )
                  Seq(n)
                case other =>
                  LOGGER.warn(s"$logPrefix Unable to parse agg.value='''$other''' as Number. Search hit is $hitInfo")
                  Nil
              }
              .toSeq
            val jsRes = GeoPoint.READS_ANY
              .reads( JsArray(lonLatArr) )
            if (jsRes.isError)
              LOGGER.error(s"$logPrefix Agg.values parsing failed for hit $hitInfo:\n $jsRes")
            jsRes.asOpt
          }
          .map(formatPoint)
          .toStream

        FeatureCollection(feats)
      }
  }


  /** Рендер инстанса одной геоточки в GeoJSON. */
  def formatPoint(gp: GeoPoint): Feature[LatLng] = {
    Feature(
      geometry    = PointGs(gp).toPlayGeoJsonGeom,
      properties  = Some(JsObject(Nil))   // TODO mapbox 0.21 косячит, если undefined то ошибка возникает.
    )
  }


  /** Сборка запроса для сбора узлов, имеющих какие-то точки в adv. */
  def geoAdvsQuery(areaOpt: Option[GeoShapeQuerable] = None): MNodeSearch = {
    new _MNodeSearch4Points {
      override def nodeTypes = Seq( MNodeTypes.Ad )
      override def outEdges: Seq[ICriteria] = {
        // 2016.sep.29: Все перечисленные предикаты имеют точки в MEdge.info.geoPoints.
        // Но выставляться эти точки начали только с сегодняшнего дня, поэтому ноды со старыми размещениями в пролёте.
        val crAdvGeo = Criteria(
          predicates = Seq(
            MPredicates.AdvGeoPlace,
            MPredicates.TaggedBy.Agt
          ),
          // Это поле не использовалось изначально, т.к. реализация areaOpt отодвинуто на будущее.
          gsIntersect = for (area <- areaOpt) yield {
            GsCriteria(
              shapes = Seq(area)
            )
          }
        )
        Seq( crAdvGeo )
      }
    }
  }




}

/** Интерфейс для поля с DI-инстансом [[MMapNodes]]. */
trait IMMapNodesDi {
  def mMapNodes: MMapNodes
}
