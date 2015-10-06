package io.suggest.model.n2.geo

import io.suggest.common.EmptyProduct
import io.suggest.model.IGenEsMappingProps
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
  }

  val empty = new MNodeGeo() {
    override def nonEmpty = false
  }


  import Fields._

  implicit val FORMAT: Format[MNodeGeo] = {
    (__ \ POINT_FN).formatNullable[GeoPoint]
      .inmap[MNodeGeo](apply, _.point)
  }


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldGeoPoint(POINT_FN, latLon = true,
        geohash = true, geohashPrefix = true,  geohashPrecision = "8",
        fieldData = GeoPointFieldData(format = GeoPointFieldDataFormats.compressed, precision = "3m")
      )
    )
  }

}


case class MNodeGeo(
  point: Option[GeoPoint] = None
)
  extends EmptyProduct
