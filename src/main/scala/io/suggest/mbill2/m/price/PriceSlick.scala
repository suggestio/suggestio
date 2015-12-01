package io.suggest.mbill2.m.price

import java.util.Currency

import slick.driver.JdbcProfile
import slick.lifted.MappedProjection

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.12.15 22:29
  * Description: Аддоны для работы с ценой через slick.
  */
trait PriceSlick {

  protected val driver: JdbcProfile

  import driver.api._

  trait ICurrencyProjection {
    def currency: MappedProjection[Currency, _]
  }

  /** Аддон для добавления отмаппленного варианта currencyCode. */
  trait CurrencyTable[X] extends Table[X] with ICurrencyProjection {

    def currencyCode: Rep[String]

    override def currency = currencyCode <> (
      Currency.getInstance(_: String),
      { c: Currency => Some(c.getCurrencyCode) }
    )

  }


  /** Поддержка маппинга цены в таблице. */
  trait PriceTable[X] extends Table[X] with ICurrencyProjection {

    def amount: Rep[Amount_t]

    def price = {
      (amount, currency) <> (MPrice.tupled, MPrice.unapply)
    }

  }

}
