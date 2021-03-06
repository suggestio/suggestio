package models.maps.umap

import io.suggest.geo.IGeoShape
import play.api.libs.json._
import play.api.libs.functional.syntax._


object Feature {

  implicit def FORMAT: Format[Feature] = (
    (__ \ "geometry").format[IGeoShape]( IGeoShape.JsonFormats.geoJsonFormat ) and
    (__ \ "properties").formatNullable[FeatureProperties] and
    (__ \ "type").format[FeatureType]
  )(apply, unlift(unapply))

}


case class Feature(
                    geometry    : IGeoShape,
                    properties  : Option[FeatureProperties] = None,
                    ftype       : FeatureType = FeatureTypes.Feature
)
