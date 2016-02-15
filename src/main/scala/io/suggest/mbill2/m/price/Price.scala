package io.suggest.mbill2.m.price

import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 15:14
 * Description: Модель стоимости, т.е. контейнера цены в у.е. и валюты этих самых у.е.
 */

trait IPrice {

  def amount    : Amount_t
  def currency  : Currency

  def currencyCode = currency.getCurrencyCode

  override def toString: String = {
    amount + " " + currencyCode
  }

}


object MPrice {

  /** Сгруппировать цены по валютам и просуммировать.
    * Часто надо получить итоговую/итоговые цены для кучи покупок. Вот тут куча цен приходит на вход.
    * @param prices Входная пачка цен.
    * @return Итератор с результирующими ценами.
    */
  def sumPricesByCurrency(prices: Seq[IPrice]): Iterator[MPrice] = {
    prices
      .groupBy {
        _.currency.getCurrencyCode
      }
      .valuesIterator
      .map { p =>
        MPrice(
          amount    = p.map(_.amount).sum,
          currency  = p.head.currency
        )
      }
  }

}

/** Дефолтовая реализация модели цены. */
case class MPrice(
  override val amount    : Amount_t,
  override val currency  : Currency
)
  extends IPrice
{
  override def toString  = super.toString
}
