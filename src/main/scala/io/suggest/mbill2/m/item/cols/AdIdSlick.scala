package io.suggest.mbill2.m.item.cols

import io.suggest.common.slick.driver.IDriver

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 15:04
 * Description: Аддон для поддержки поля ad_id в slick-моделях.
 */
trait AdIdSlick extends IDriver {

  import driver.api._

  def AD_ID_FN = "ad_id"

  trait AdIdColumn { that: Table[_] =>
    def adId = column[String](AD_ID_FN)
  }

}


trait IAdId {
  def adId: String
}
