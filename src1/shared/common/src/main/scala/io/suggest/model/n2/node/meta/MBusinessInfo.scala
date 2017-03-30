package io.suggest.model.n2.node.meta

import boopickle.Default._
import io.suggest.common.empty.{EmptyProduct, IEmpty}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:58
  * Description: Кросс-платформенная модель бизнес-инфы по узлу.
  */

object MBusinessInfo extends IEmpty {

  override type T = MBusinessInfo

  /** Частоиспользуемый пустой экземпляр модели [[MBusinessInfo]]. */
  override val empty: MBusinessInfo = {
    new MBusinessInfo() {
      override def nonEmpty = false
    }
  }

  implicit val mBusinessInfoPickler: Pickler[MBusinessInfo] = {
    generatePickler[MBusinessInfo]
  }

}


case class MBusinessInfo(
  siteUrl             : Option[String]  = None,
  audienceDescr       : Option[String]  = None,
  humanTrafficAvg     : Option[Int]     = None,
  info                : Option[String]  = None
)
  extends EmptyProduct
