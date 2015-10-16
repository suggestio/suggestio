package io.suggest.model.n2.geo

import io.suggest.common.EmptyProduct
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.geo.GeoPoint
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 17:28
 * Description: JSON-модель для представления геоданных узлов [[io.suggest.model.n2.node.MNode]].
 */
object MNodeGeo extends IGenEsMappingProps {

  object Fields {
    val POINT_FN = "p"
    val SHAPE_FN = "s"
  }

  val empty = new MNodeGeo() {
    override def nonEmpty = false
  }


  import Fields._

  implicit val FORMAT: Format[MNodeGeo] = (
    (__ \ POINT_FN).formatNullable[GeoPoint] and
    (__ \ SHAPE_FN).formatNullable[MGeoShape]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldGeoPoint(POINT_FN, latLon = true,
        geohash = true, geohashPrefix = true,  geohashPrecision = "8",
        fieldData = GeoPointFieldData(format = GeoPointFieldDataFormats.compressed, precision = "3m")
      ),
      FieldObject(SHAPE_FN, enabled = true, properties = MGeoShape.generateMappingProps)
    )
  }

}


case class MNodeGeo(
  point: Option[GeoPoint] = None,
  shape: Option[MGeoShape] = None
)
  extends EmptyProduct
