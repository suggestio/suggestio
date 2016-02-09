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


/** Дефолтовая реализация модели цены. */
case class MPrice(
  override val amount    : Amount_t,
  override val currency  : Currency
)
  extends IPrice
{
  override def toString  = super.toString
}
