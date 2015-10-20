package io.suggest.model.n2.geo

import io.suggest.common.EmptyProduct
import io.suggest.model.PrefixedFn
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.geo.GeoPoint
import io.suggest.ym.model.NodeGeoLevel
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

    object Shape extends PrefixedFn {
      val SHAPE_FN = "sh"
      override protected def _PARENT_FN: String = SHAPE_FN

      def geoShapeFullFn(ngl: NodeGeoLevel): String = {
        _fullFn( MGeoShape.Fields.shapeFn(ngl) )
      }
      def SHAPE_GLEVEL_FN = _fullFn( MGeoShape.Fields.GLEVEL_FN )
    }
  }

  val empty: MNodeGeo = {
    new MNodeGeo() {
      override def nonEmpty = false
    }
  }


  implicit val FORMAT: Format[MNodeGeo] = (
    (__ \ Fields.POINT_FN).formatNullable[GeoPoint] and
    (__ \ Fields.Shape.SHAPE_FN).formatNullable[MGeoShape]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldGeoPoint(Fields.POINT_FN, latLon = true,
        geohash = true, geohashPrefix = true,  geohashPrecision = "8",
        fieldData = GeoPointFieldData(format = GeoPointFieldDataFormats.compressed, precision = "3m")
      ),
      FieldNestedObject(Fields.Shape.SHAPE_FN, enabled = true, properties = MGeoShape.generateMappingProps)
    )
  }

}


case class MNodeGeo(
  point: Option[GeoPoint] = None,
  shape: Option[MGeoShape] = None
)
  extends EmptyProduct
