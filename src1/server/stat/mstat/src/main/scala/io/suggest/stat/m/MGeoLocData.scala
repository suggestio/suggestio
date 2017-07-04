package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.geo.MGeoPoint
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.geo.GeoPoint.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 15:56
  * Description: Суб-модель для представления каких-то данных геолокации.
  */
object MGeoLocData extends IGenEsMappingProps with IEmpty {

  override type T = MGeoLocData

  object Fields {

    val COORDS_FN     = "coords"
    val ACCURACY_FN   = "accuracy"
    val TOWN_FN       = "town"
    val COUNTRY_FN    = "country"

  }


  import Fields._

  implicit val FORMAT: OFormat[MGeoLocData] = (
    // TODO Почему-то этот код не видит GeoPoint.Implicits.FORMAT, поэтому тут он вписан явно.
    (__ \ COORDS_FN).formatNullable[MGeoPoint] and
    (__ \ ACCURACY_FN).formatNullable[Int] and
    (__ \ TOWN_FN).formatNullable[String] and
    (__ \ COUNTRY_FN).formatNullable[String]
  )(apply, unlift(unapply))


  override def empty = MGeoLocData()


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldGeoPoint( COORDS_FN ),
      FieldNumber(ACCURACY_FN, fieldType = DocFieldTypes.integer, index = true, include_in_all = false),
      FieldKeyword(TOWN_FN, index = true, include_in_all = true),
      FieldKeyword(COUNTRY_FN, index = true, include_in_all = true)
    )
  }

}


/** Класс модели геоинформации. */
case class MGeoLocData(
  coords    : Option[MGeoPoint] = None,
  accuracy  : Option[Int]       = None,
  town      : Option[String]    = None,
  country   : Option[String]    = None
)
  extends EmptyProduct
