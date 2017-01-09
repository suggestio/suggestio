package io.suggest.model.n2.node.meta

import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.model.es.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 15:04
 * Description: Модель метаданных по описанию делишек узла: инфа о товарах, посетителях, сайте и т.д.
 */
object MBusinessInfo extends IGenEsMappingProps with IEmpty {

  override type T = MBusinessInfo

  object Fields {
    val SITE_URL_FN             = "su"
    val AUDIENCE_DESCR_FN       = "ad"
    val HUMAN_TRAFFIC_AVG_FN    = "ht"

    /** Имя поля для описания серьезного бизнеса: Business DESCRiption. */
    val BDESCR_FN               = "bd"
  }

  /** Частоиспользуемый пустой экземпляр модели [[MBusinessInfo]]. */
  override val empty: MBusinessInfo = {
    new MBusinessInfo() {
      override def nonEmpty = false
    }
  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MBusinessInfo] = (
    (__ \ SITE_URL_FN).formatNullable[String] and
    (__ \ AUDIENCE_DESCR_FN).formatNullable[String] and
    (__ \ HUMAN_TRAFFIC_AVG_FN).formatNullable[Int] and
    (__ \ BDESCR_FN).formatNullable[String]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._
  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(SITE_URL_FN, index = FieldIndexingVariants.analyzed, include_in_all = true, boost = Some(0.33F)),
      FieldString(AUDIENCE_DESCR_FN, index = FieldIndexingVariants.analyzed, include_in_all = false),
      FieldNumber(HUMAN_TRAFFIC_AVG_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(BDESCR_FN, index = FieldIndexingVariants.analyzed, include_in_all = true, boost = Some(0.1F))
    )
  }

}


case class MBusinessInfo(
  siteUrl             : Option[String]  = None,
  audienceDescr       : Option[String]  = None,
  humanTrafficAvg     : Option[Int]     = None,
  info                : Option[String]  = None
)
  extends EmptyProduct
