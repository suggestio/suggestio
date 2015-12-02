package io.suggest.mbill2.m.item.adv.cols

import io.suggest.common.slick.driver.IDriver

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 16:19
 * Description: Поддержка поля с id ресивера (колонка rcvr_id).
 */
trait RcvrIdSlick extends IDriver {

  import driver.api._

  def RCVR_ID_FN = "rcvr_id"

  trait RcvrIdColumn { that: Table[_] =>
    def rcvrId = column[String](RCVR_ID_FN)
  }

}

trait IRcvrId {
  def rcvrId: String
}
