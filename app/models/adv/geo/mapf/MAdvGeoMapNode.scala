package models.adv.geo.mapf

import io.suggest.adv.geo.AdvGeoConstants.AdnNodes._
import io.suggest.model.geo.GeoPoint
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
  point   : GeoPoint,
  props   : MAdvGeoMapNodeProps
) {

  /** Конвертация в GeoJSON-классы. */
  def toGeoJson = {
    Feature(
      geometry = Point(
        coordinates = point.toLngLat
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
    (__ \ ICON_URL_FN).writeNullable[String] and
    (__ \ BG_COLOR_FN).writeNullable[String]
  )(unlift(unapply))

}

/** Модель пропертей узлов, отображаемых на карте.
  *
  * @param nodeId id узла для возможности запроса попапа или какие-то ещё действия производить.
  * @param hint Подсказка при наведении на узел.
  * @param iconUrl Логотип узла, отображаемый на карте.
  */
case class MAdvGeoMapNodeProps(
  nodeId  : String,
  hint    : Option[String],
  iconUrl : Option[String] = None,
  bgColor : Option[String] = None
)
