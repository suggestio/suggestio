package io.suggest.model.n2.geo

import io.suggest.common.empty.{IEmpty, EmptyProduct}
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
object MNodeGeo extends IGenEsMappingProps with IEmpty {

  override type T = MNodeGeo

  object Fields {

    val POINT_FN = "p"
    val SHAPE_FN = "sh"

  }

  override val empty: MNodeGeo = {
    new MNodeGeo() {
      override def nonEmpty = false
    }
  }


  implicit val FORMAT: Format[MNodeGeo] = (
    (__ \ Fields.POINT_FN).formatNullable[GeoPoint] and
    (__ \ Fields.SHAPE_FN).formatNullable[Seq[MGeoShape]]
      .inmap [Seq[MGeoShape]] (
        _.getOrElse(Nil),
        { gss => if (gss.isEmpty) None else Some(gss) }
      )
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldGeoPoint(
        id                = Fields.POINT_FN,
        latLon            = true,
        geohash           = true,
        geohashPrefix     = true,
        geohashPrecision  = "8",
        fieldData         = GeoPointFieldData(format = GeoPointFieldDataFormats.compressed, precision = "3m")
      ),
      FieldNestedObject(Fields.SHAPE_FN, enabled = true, properties = MGeoShape.generateMappingProps)
    )
  }

}


case class MNodeGeo(
  point     : Option[GeoPoint]  = None,
  shapes    : Seq[MGeoShape]    = Nil
)
  extends EmptyProduct
{

  def nextShapeId: Int = {
    if (shapes.isEmpty) {
      0
    } else {
      shapes.iterator
        .map(_.id)
        .max + 1
    }
  }

  def updateShapeSeq(mgs2: MGeoShape): Seq[MGeoShape] = {
    shapes
      .iterator
      .filter { _.id != mgs2.id }
      .++( Iterator(mgs2) )
      .toSeq
  }

  def updateShape(mgs2: MGeoShape): MNodeGeo = {
    copy(
      shapes = updateShapeSeq(mgs2)
    )
  }

  def findShape(gsId: Int): Option[MGeoShape] = {
    shapes.find(_.id == gsId)
  }

}
