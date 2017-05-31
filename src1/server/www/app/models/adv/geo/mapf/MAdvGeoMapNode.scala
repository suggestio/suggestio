package models.adv.geo.mapf

import io.suggest.adv.geo.AdvGeoConstants.AdnNodes._
import io.suggest.geo.{GeoPoint, MGeoPoint}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.extras.geojson.{Feature, Point}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 15:10
  * Description: Модель одного узла на карте узлов.
  * Сериализуется она не напрямую, а через toGeoJson().
  * Десериализация не поддерживается за ненадобностью.
  */


/**
  * Инстанс модели данных об одном узле на карте узлов размещения карточки.
  *
  * @param point Координаты узла.
  */
case class MAdvGeoMapNode(
  point   : MGeoPoint,
  props   : MAdvGeoMapNodeProps
) {

  /** Конвертация в GeoJSON-классы. */
  def toGeoJson = {
    Feature(
      geometry = Point(
        coordinates = GeoPoint.toLngLat(point)
      ),
      properties = Some {
        implicitly[OWrites[MAdvGeoMapNodeProps]]
          .writes(props)
      }
    )
  }

}



/** Поддержка модели пропертей узла-ресивера для карты рекламного размещения. */
object MAdvGeoMapNodeProps {

  /** Поддержка сериализации в JSON. */
  implicit val WRITES: OWrites[MAdvGeoMapNodeProps] = (
    (__ \ NODE_ID_FN).write[String] and
    (__ \ HINT_FN).writeNullable[String] and
    (__ \ ICON_FN).writeNullable[MIconInfo] and
    (__ \ BG_COLOR_FN).writeNullable[String] and
    (__ \ CIRCLE_RADIUS_M_FN).writeNullable[Double]
  )(unlift(unapply))

}

/** Модель пропертей узлов, отображаемых на карте.
  *
  * @param nodeId id узла для возможности запроса попапа или какие-то ещё действия производить.
  * @param hint Подсказка при наведении на узел.
  * @param icon Логотип узла, отображаемый на карте.
  */
case class MAdvGeoMapNodeProps(
  nodeId          : String,
  hint            : Option[String],
  icon            : Option[MIconInfo] = None,
  bgColor         : Option[String]    = None,
  circleRadiusM   : Option[Double]    = None
)


import Icon._

/** Поддержка модели данных по иконке узла на карте. */
object MIconInfo {
  implicit val WRITES: OWrites[MIconInfo] = (
    (__ \ URL_FN).write[String] and
    (__ \ WIDTH_FN).write[Int] and
    (__ \ HEIGHT_FN).write[Int]
  )(unlift(unapply))
}
/** Класс JSON-модели инфы по иконке узла для карты размещений. */
case class MIconInfo(
  url     : String,
  width   : Int,
  height  : Int
)