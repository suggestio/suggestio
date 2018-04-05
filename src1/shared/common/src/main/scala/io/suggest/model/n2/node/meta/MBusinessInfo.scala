package io.suggest.model.n2.node.meta

import boopickle.Default._
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:58
  * Description: Кросс-платформенная модель бизнес-инфы по узлу.
  */

object MBusinessInfo extends IEmpty {

  override type T = MBusinessInfo

  object Fields {
    val SITE_URL_FN             = "su"
    val AUDIENCE_DESCR_FN       = "ad"
    val HUMAN_TRAFFIC_AVG_FN    = "ht"
    /** Имя поля для описания серьезного бизнеса: Business DESCRiption. */
    val BDESCR_FN               = "bd"
  }

  /** Поддержка JSON. */
  implicit val MBUSINESS_INFO_FORMAT: OFormat[MBusinessInfo] = (
    (__ \ Fields.SITE_URL_FN).formatNullable[String] and
    (__ \ Fields.AUDIENCE_DESCR_FN).formatNullable[String] and
    (__ \ Fields.HUMAN_TRAFFIC_AVG_FN).formatNullable[Int] and
    (__ \ Fields.BDESCR_FN).formatNullable[String]
  )(apply, unlift(unapply))


  /** Частоиспользуемый пустой экземпляр модели [[MBusinessInfo]]. */
  override val empty = MBusinessInfo()

  implicit val mBusinessInfoPickler: Pickler[MBusinessInfo] = {
    generatePickler[MBusinessInfo]
  }

  implicit def univEq: UnivEq[MBusinessInfo] = UnivEq.derive

}


case class MBusinessInfo(
  siteUrl             : Option[String]  = None,
  audienceDescr       : Option[String]  = None,
  humanTrafficAvg     : Option[Int]     = None,
  info                : Option[String]  = None
)
  extends EmptyProduct
