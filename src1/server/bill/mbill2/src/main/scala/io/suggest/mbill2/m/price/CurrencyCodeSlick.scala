package io.suggest.mbill2.m.price

import io.suggest.slick.profile.IProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 11:24
 * Description: Поддержка частого поля currency_code в таблице.
 */
trait CurrencyCodeSlick extends IProfile {

  import profile.api._

  def CURRENCY_CODE_FN = "currency_code"

  trait CurrencyCodeColumn { that: Table[_] =>
    def currencyCode  = column[String](CURRENCY_CODE_FN)
  }

}
