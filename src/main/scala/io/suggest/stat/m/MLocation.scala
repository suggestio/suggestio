package io.suggest.stat.m

import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.model.es.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 15:47
  * Description: Под-модель для представления статистики, связанной с географией.
  */
object MLocation extends IGenEsMappingProps with IEmpty {

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
    (__ \ GEO_IP_FN).formatNullable[MGeoLocData]
      .inmap( EmptyUtil.opt2ImplMEmptyF(MGeoLocData), EmptyUtil.implEmpty2OptF[MGeoLocData] ) and
    (__ \ GEO_LOC_FN).formatNullable[MGeoLocData]
      .inmap( EmptyUtil.opt2ImplMEmptyF(MGeoLocData), EmptyUtil.implEmpty2OptF[MGeoLocData] )
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    val glDataProps = MGeoLocData.generateMappingProps
    List(
      FieldObject(GEO_IP_FN, enabled = true, properties = glDataProps),
      FieldObject(GEO_LOC_FN, enabled = true, properties = glDataProps)
    )
  }

}


/** Класс модели по локациям. */
case class MLocation(
  geo             : MGeoLocData       = MGeoLocData.empty,
  geoip           : MGeoLocData       = MGeoLocData.empty
)
  extends EmptyProduct
