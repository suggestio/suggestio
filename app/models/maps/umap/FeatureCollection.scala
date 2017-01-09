package models.maps.umap

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.16 15:13
  * Description: JSON-Модель для объектов FeatureCollection.
  */
object FeatureCollection {

  implicit def FORMAT: Format[FeatureCollection] = (
    (__ \ "_storage").format[Layer] and
    (__ \ "features").format[Seq[Feature]] and
    (__ \ "type").format[FeatureType]
  )(apply, unlift(unapply))

}


case class FeatureCollection(
  storage     : Layer,
  features    : Seq[Feature],
  ftype       : FeatureType = FeatureTypes.FeatureCollection
)
