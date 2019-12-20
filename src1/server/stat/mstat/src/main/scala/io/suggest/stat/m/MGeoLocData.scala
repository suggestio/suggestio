package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
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
object MGeoLocData
  extends IEsMappingProps
  with IEmpty
{

  override type T = MGeoLocData

  object Fields {

    val COORDS_FN     = "coords"
    val ACCURACY_FN   = "accuracy"
    val TOWN_FN       = "town"
    val COUNTRY_FN    = "country"

  }


  import Fields._

  implicit val FORMAT: OFormat[MGeoLocData] = (
    (__ \ COORDS_FN).formatNullable[MGeoPoint] and
    (__ \ ACCURACY_FN).formatNullable[Int] and
    (__ \ TOWN_FN).formatNullable[String] and
    (__ \ COUNTRY_FN).formatNullable[String]
  )(apply, unlift(unapply))

  override def empty = MGeoLocData()

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.COORDS_FN -> FGeoPoint.indexedJs,
      F.ACCURACY_FN -> FNumber(
        typ = DocFieldTypes.Integer,
        index = someTrue,
      ),
      F.TOWN_FN -> FKeyWord.indexedJs,
      F.COUNTRY_FN -> FKeyWord.indexedJs,
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
