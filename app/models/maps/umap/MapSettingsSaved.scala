package models.maps.umap

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.16 15:00
  * Description: JSON-модель ответа на запроса сохранения настроек карты.
  */
object MapSettingsSaved {

  implicit def FORMAT: Format[MapSettingsSaved] = (
    (__ \ "url").format[String] and
    (__ \ "info").format[String] and
    (__ \ "id").format[Int]
  )(apply, unlift(unapply))

}

case class MapSettingsSaved(
  url   : String,
  info  : String,
  id    : Int
)
