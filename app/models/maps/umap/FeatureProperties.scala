package models.maps.umap

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.16 15:11
  * Description: JSON-модель описания пропертей одной фичи.
  */
object FeatureProperties {

  implicit val FORMAT: Format[FeatureProperties] = (
    (__ \ "name").format[String] and
    (__ \ "description").format[String]
  )(apply, unlift(unapply))

}

case class FeatureProperties(
  name        : String,
  description : String
) {

  /** Передача id узлов происходит внутри description. */
  def nodeId = description

}
