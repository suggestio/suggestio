package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 15:47
  * Description: Под-модель для представления статистики, связанной с географией.
  */
object MLocation
  extends IEsMappingProps
  with IEmpty
{

  override type T = MLocation

  override def empty = MLocation()

  object Fields {

    val GEO_IP_FN               = "geoip"
    val GEO_LOC_FN              = "geo"

    def BEACONS_FN              = "beacon"

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[T] = (
    (__ \ GEO_LOC_FN).formatNullable[MGeoLocData]
      .inmap( EmptyUtil.opt2ImplMEmptyF(MGeoLocData), EmptyUtil.implEmpty2OptF[MGeoLocData] ) and
    (__ \ GEO_IP_FN).formatNullable[MGeoLocData]
      .inmap( EmptyUtil.opt2ImplMEmptyF(MGeoLocData), EmptyUtil.implEmpty2OptF[MGeoLocData] )
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val glDataProps = Json.toJsObject(
      FObject.plain( MGeoLocData ),
    )
    val F = Fields
    Json.obj(
      F.GEO_LOC_FN -> glDataProps,
      F.GEO_IP_FN  -> glDataProps,
    )
  }

}


/** Класс модели по локациям. */
case class MLocation(
  geo             : MGeoLocData       = MGeoLocData.empty,
  geoIp           : MGeoLocData       = MGeoLocData.empty
)
  extends EmptyProduct
