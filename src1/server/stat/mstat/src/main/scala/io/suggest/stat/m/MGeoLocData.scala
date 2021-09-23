package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.geo.MGeoPoint
import play.api.libs.json._
import play.api.libs.functional.syntax._

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


  implicit def geoLocDataJson: OFormat[MGeoLocData] = {
    val F = Fields
    (
      (__ \ F.COORDS_FN).formatNullable[Seq[MGeoPoint]]
        .inmap[Seq[MGeoPoint]](
          EmptyUtil.opt2ImplEmpty1F( Nil ),
          seq => Option.when(seq.nonEmpty)(seq)
        ) and
      (__ \ F.ACCURACY_FN).formatNullable[Seq[Int]]
        .inmap[Seq[Int]](
          EmptyUtil.opt2ImplEmpty1F( Nil ),
          seq => Option.when(seq.nonEmpty)(seq)
        ) and
      (__ \ F.TOWN_FN).formatNullable[Seq[String]]
        .inmap[Seq[String]](
          EmptyUtil.opt2ImplEmpty1F( Nil ),
          seq => Option.when(seq.nonEmpty)(seq)
        ) and
      (__ \ F.COUNTRY_FN).formatNullable[Seq[String]]
        .inmap[Seq[String]](
          EmptyUtil.opt2ImplEmpty1F( Nil ),
          seq => Option.when(seq.nonEmpty)(seq)
        )
    )(apply, unlift(unapply))
  }

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
final case class MGeoLocData(
  coords    : Seq[MGeoPoint]    = Nil,
  accuracy  : Seq[Int]          = Nil,
  town      : Seq[String]       = Nil,
  country   : Seq[String]       = Nil,
)
  extends EmptyProduct
{

  def toStringSb(sb: StringBuilder = new StringBuilder(64)): StringBuilder = {
    sb.append('(')
    for (coo <- coords) {
      coo.toHumanFriendlySb( sb )
      sb.append(' ')
    }
    for (accu <- accuracy) {
      sb.append(accu)
        .append('m')
        .append(' ')
    }
    for (t <- town) {
      sb.append( t )
        .append( ' ' )
    }
    for (c <- country)
      sb.append(c)

    sb.append(')')
  }

  override def toString = toStringSb().toString()

}