package util.geo.umap

import java.{lang => jl, util => ju}

import io.suggest.model.EsModel.{intParser, stringParser}
import io.suggest.model.geo.{GsTypes, PolygonGs, MultiPolygonGs, GeoShape}
import io.suggest.ym.model.NodeGeoLevels
import models.{MAdnNodeGeo, NodeGeoLevel}
import org.elasticsearch.common.xcontent.XContentHelper
import play.api.Play.{current, configuration}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.14 9:14
 * Description: Утиль для работы с фронтендом UMap (leaflet-storage).
 */
object UmapUtil {

  /** 2014.09.23: Umap не поддерживает тип фигур MultiPolygon. Можно их сплиттить на полигоны. */
  val SPLIT_MULTIPOLYGON: Boolean = configuration.getBoolean("umap.mpoly.split") getOrElse true

  /** Название поля в форме карты, которое содержит id узла. */
  val ADN_ID_SHAPE_FORM_FN = "description"

  /**
   * Десериализация json-выхлопа, присланного фронтендом.
   * @param bytes Байты json'а.
   * @return Распарсенный ответ.
   */
  def deserializeFromBytes(bytes: Array[Byte]): Option[UmapDataLayer] = {
    // Используем XCH для парсинга, т.к. он выдаёт нужный формат данных для парсеров гео-моделей. Но вообще это плохая идея.
    val esMap = XContentHelper.convertToMap(bytes, false).v2()
    Option(esMap get "type")
      .map(stringParser)
      .filter(_ equalsIgnoreCase "FeatureCollection")
      .flatMap { _ => Option(esMap get "_storage") }
      .flatMap {
        case jmap: ju.Map[_, _] =>
          Option(jmap get "id").map(intParser).flatMap(NodeGeoLevels.maybeWithId)
      }
      .flatMap { ngl =>
        // Парсим объекты карты (фичи)
        Option(esMap get "features").map {
          case featuresRaw: jl.Iterable[_] =>
            val features = featuresRaw.iterator().map {
              case featureRaw: ju.Map[_,_] =>
                val gsOpt = GeoShape.deserialize(featureRaw get "geometry") map { gs =>
                  UmapFeature(
                    geometry = gs,
                    properties = Option(featureRaw get "properties").map {
                      case pjmap: ju.Map[_,_]  =>  pjmap.toMap.asInstanceOf[Map[String, AnyRef]]
                    }.getOrElse(Map.empty)
                  )
                }
                gsOpt.get   // При ошибке парсинга должен быть экзепшен, чтобы не потерять элементы карты.
            }.toSeq
            UmapDataLayer(ngl, features)
        }
      }
  }


  /**
   * Из-за особенностей Umap, бывает необходимо допилить список фигур перед рендером json-слоя.
   * @param geos Коллекция или итератор фигур, которые надо рендерить в слое.
   * @return Обновлённый или тот же список фигур в исходном порядке.
   */
  def prepareDataLayerGeos(geos: TraversableOnce[MAdnNodeGeo]): TraversableOnce[MAdnNodeGeo] = {
    // Если включена какая-то опция модификации списка geo-фигур, то нужно запустить обход списка.
    if (SPLIT_MULTIPOLYGON) {
      geos.flatMap { geo =>
        // Если включена трансформация мультирополигонов (Umap их не поддерживала), то размножаем инстанс шейпа на полигоны:
        if (SPLIT_MULTIPOLYGON && geo.shape.shapeType == GsTypes.multipolygon) {
          val mpoly = geo.shape.asInstanceOf[MultiPolygonGs]
          mpoly.polygons.map { poly =>
            geo.copy(shape = poly, id = None)
          }
        } else {
          Seq(geo)
        }
      }

    } else {
      // Ничего менять не требуется.
      geos
    }
  }

}

case class UmapFeature(geometry: GeoShape, properties: Map[String, AnyRef]) {
  val adnIdOpt = properties.get(UmapUtil.ADN_ID_SHAPE_FORM_FN).map(stringParser)
}

case class UmapDataLayer(ngl: NodeGeoLevel, features: Seq[UmapFeature])

