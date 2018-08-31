package io.suggest.model.n2.geo

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.geo.MGeoPoint
import io.suggest.geo.GeoPoint.Implicits._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 17:28
 * Description: JSON-модель для представления геоданных узлов [[io.suggest.model.n2.node.MNode]].
 */

// TODO Эта модель неактуальна. Так или иначе, её суть переехал в MEdge.info.shape и point.
// Надо портировать зависимый код и удалить её вообще.
object MNodeGeo extends IGenEsMappingProps with IEmpty {

  override type T = MNodeGeo

  object Fields {

    val POINT_FN = "p"

  }

  override val empty: MNodeGeo = {
    new MNodeGeo() {
      override def nonEmpty = false
    }
  }

  implicit val FORMAT: Format[MNodeGeo] = {
    (__ \ Fields.POINT_FN).formatNullable[MGeoPoint]
      .inmap [MNodeGeo] (
        apply, _.point
      )
  }


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldGeoPoint( Fields.POINT_FN )
    )
  }

}


case class MNodeGeo(
  point     : Option[MGeoPoint]  = None
)
  extends EmptyProduct
