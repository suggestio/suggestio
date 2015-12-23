package io.suggest.mbill2.m.txn.comis

import io.suggest.common.slick.driver.IDriver
import io.suggest.mbill2.m.price.Amount_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.12.15 15:24
 * Description: Поддержка поля объема комиссионного списания.
 */

trait ComissionAmountOptSlick extends IDriver {

  import driver.api._

  def COMISSION_AMOUNT_FN       = "comission_amount"

  trait ComissionAmountOpt { that: Table[_] =>

    /** Поле объема опциональной комиссии по платежу. */
    def comissionAmountOpt   = column[Option[Amount_t]](COMISSION_AMOUNT_FN)
  }

}
