package io.suggest.bill

import boopickle.Default._

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
    implicit val priceP = MPrice.mPriceBp
    generatePickler[MGetPriceResp]
  }

}


/**
  * Класс модели ответа на запрос рассчёта стоимости [размещения].
  * @param prices Данные по стоимостям.
  */
case class MGetPriceResp(
                          prices: Seq[MPrice]
                        )
