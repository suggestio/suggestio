package models.msc.map

import io.suggest.sc.map.ScMapConstants.Nodes._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.extras.geojson.{FeatureCollection, LatLng}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.16 10:04
  * Description: Контейнер JSON-ответа для данных одного mapbox-сорса из экшена renderMapNodesLayer().
  */
object MNodesSource {

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MNodesSource] = (
    (__ \ SRC_NAME_FN).format[String] and
    (__ \ IS_CLUSTERED_FN).format[Boolean] and
    (__ \ SRC_DATA_FN).format[FeatureCollection[LatLng]]
  )(apply _, unlift(unapply))

}


/**
  * Класс модели JSON-ответа рендера GeoJSON-слоя узлов карты.
  *
  * @param srcName Имя сорса карты, в который заливаются эти данные.
  * @param clustered Является ли рендер кластерным?
  * @param features Содержимое слоя.
  */
case class MNodesSource(
  srcName   : String,
  clustered : Boolean,
  features  : FeatureCollection[LatLng]
)
