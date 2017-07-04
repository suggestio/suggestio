package io.suggest.model.n2.node.meta

import io.suggest.es.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 15:04
 * Description: Модель метаданных по описанию делишек узла: инфа о товарах, посетителях, сайте и т.д.
 */
object MBusinessInfoEs extends IGenEsMappingProps {

  object Fields {
    val SITE_URL_FN             = "su"
    val AUDIENCE_DESCR_FN       = "ad"
    val HUMAN_TRAFFIC_AVG_FN    = "ht"

    /** Имя поля для описания серьезного бизнеса: Business DESCRiption. */
    val BDESCR_FN               = "bd"
  }


  import Fields._

  /** Поддержка JSON. */
  implicit val MBUSINESS_INFO_FORMAT: OFormat[MBusinessInfo] = (
    (__ \ SITE_URL_FN).formatNullable[String] and
    (__ \ AUDIENCE_DESCR_FN).formatNullable[String] and
    (__ \ HUMAN_TRAFFIC_AVG_FN).formatNullable[Int] and
    (__ \ BDESCR_FN).formatNullable[String]
  )(MBusinessInfo.apply, unlift(MBusinessInfo.unapply))


  import io.suggest.es.util.SioEsUtil._
  override def generateMappingProps: List[DocField] = {
    List(
      FieldText(SITE_URL_FN, index = true, include_in_all = true, boost = Some(0.33F)),
      FieldText(AUDIENCE_DESCR_FN, index = true, include_in_all = false),
      FieldNumber(HUMAN_TRAFFIC_AVG_FN, fieldType = DocFieldTypes.integer, index = true, include_in_all = false),
      FieldText(BDESCR_FN, index = true, include_in_all = true, boost = Some(0.1F))
    )
  }

}
