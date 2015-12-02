package io.suggest.mbill2.m.item.cols

import io.suggest.common.slick.driver.IDriver

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.15 22:20
  * Description:
  */
trait RcvrIdOptSlick extends IDriver {

  import driver.api._

  def RCVR_ID_FN = "rcvr_id"

  trait RcvrIdOptColumn { that: Table[_] =>
    def rcvrIdOpt = column[Option[String]](RCVR_ID_FN)
  }

}

trait IRcvrIdOpt {
  def rcvrIdOpt: Option[String]
}
