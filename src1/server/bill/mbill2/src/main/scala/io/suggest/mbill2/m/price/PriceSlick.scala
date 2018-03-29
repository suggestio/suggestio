package io.suggest.mbill2.m.price

import io.suggest.bill.{Amount_t, MCurrencies, MCurrency, MPrice}
import io.suggest.slick.profile.IProfile
import slick.lifted.MappedProjection

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 22:29
 * Description: Аддоны для работы с ценой через slick.
 */
trait PriceSlick extends IProfile {

  import profile.api._

  trait ICurrencyProjection {
    def currency: MappedProjection[MCurrency, _]
  }

  /** Аддон для добавления отмаппленного варианта currencyCode. */
  trait CurrencyColumn extends ICurrencyProjection {

    def currencyCode: Rep[String]

    override def currency = currencyCode <> (
      MCurrencies.withValue(_: String),
      { c: MCurrency => Some(c.currencyCode) }
    )

  }


  /** Поддержка маппинга цены в таблице. */
  trait PriceColumn extends ICurrencyProjection {

    def amount: Rep[Amount_t]

    def price = {
      (amount, currency) <> ((MPrice.apply2 _).tupled, MPrice.unapply2)
    }

  }

}
