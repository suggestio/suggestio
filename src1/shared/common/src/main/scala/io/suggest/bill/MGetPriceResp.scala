package io.suggest.bill

import boopickle.Default._
import io.suggest.bill.price.dsl.IPriceDslTerm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.01.17 18:44
  * Description: Кросс-платформенная модель ответа на запрос рассчёта стоимости чего-либо (размещения).
  *
  * Модель идёт на замену server-only модели MAdvPricing, слишком сильно зависящей от java.util.Currency
  * и особенностей старого биллинга.
  */

object MGetPriceResp {

  implicit val pickler: Pickler[MGetPriceResp] = {
    implicit val priceP = MPrice.mPricePickler
    implicit val iPriceDslTermP = IPriceDslTerm.iPriceDslTermPickler
    generatePickler[MGetPriceResp]
  }

}


/**
  * Класс модели ответа на запрос рассчёта стоимости [размещения].
  * @param prices Данные по стоимостям.
  *               Iterable для упрощения некоторого кода, было изначально Seq[].
  *               Ситуация, когда несколько валют, довольно маловероятна.
  * @param priceDsl Терм PriceDsl, если есть.
  */
case class MGetPriceResp(
                          prices    : Iterable[MPrice],
                          priceDsl  : Option[IPriceDslTerm]   = None
                        ) {

  def withPrices(prices2: Iterable[MPrice]) = copy(prices = prices2)

  override def toString: String = {
    prices.mkString("$[", ",", "]")
  }

}
