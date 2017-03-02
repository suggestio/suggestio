package io.suggest.mbill2.m.price

import io.suggest.bill.Amount_t
import io.suggest.slick.profile.IProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 11:21
 * Description: Поддержа поля amount.
 */
trait AmountSlick extends IProfile {

  import profile.api._

  def AMOUNT_FN = "amount"

  trait AmountColumn { that: Table[_] =>
    def amount = column[Amount_t](AMOUNT_FN)
  }

}
