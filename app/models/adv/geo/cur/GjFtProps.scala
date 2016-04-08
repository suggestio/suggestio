package models.adv.geo.cur

import io.suggest.adv.geo.AdvGeoConstants.GjFtPropsC._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.04.16 17:31
  * Description: JSON-модель пропертей внутри GeoJSON Feature с данными для рендера.
  */

object GjFtProps {

  /** Поддержка JSON. По факту системе нужен только Writes. */
  implicit val FORMAT: OFormat[GjFtProps] = (
    (__ \ FILL_COLOR_FN).formatNullable[String] and
    (__ \ FILL_OPACITY_FN).formatNullable[Float] and
    (__ \ RADIUS_FN).formatNullable[Double] and
    (__ \ POPUP_CONTENT_FN).formatNullable[String]
  )(apply, unlift(unapply))

}


/** Класс модели пропертисов внутри GeoJSON Feature, описывающих элемент для рендера
  * в рамках формы-карты размещения на карте. */
case class GjFtProps(
  fillColor       : Option[String]  = None,
  fillOpacity     : Option[Float]   = None,
  radiusM         : Option[Double]  = None,
  popupContent    : Option[String]  = None
)
