package io.suggest.mbill2.m.price

import java.util.Currency
import io.suggest.common.slick.driver.IDriver
import slick.lifted.MappedProjection

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 22:29
 * Description: Аддоны для работы с ценой через slick.
 */
trait PriceSlick extends IDriver {

  import driver.api._

  trait ICurrencyProjection {
    def currency: MappedProjection[Currency, _]
  }

  /** Аддон для добавления отмаппленного варианта currencyCode. */
  trait CurrencyColumn extends ICurrencyProjection {

    def currencyCode: Rep[String]

    override def currency = currencyCode <> (
      Currency.getInstance(_: String),
      { c: Currency => Some(c.getCurrencyCode) }
    )

  }


  /** Поддержка маппинга цены в таблице. */
  trait PriceColumn extends ICurrencyProjection {

    def amount: Rep[Amount_t]

    def price = {
      (amount, currency) <> ((MPrice.apply _).tupled, MPrice.unapply)
    }

  }

}
