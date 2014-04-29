package models

import play.api.mvc.Call
import java.util.Currency
import io.suggest.ym.model.common.IBlockMeta
import io.suggest.ym.model.ad.IOffers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.14 17:09
 * Description: Моделе-подобное барахло, которые в основном нужно для шаблонов.
 */

object CurrencyCodeOpt {
  val CURRENCY_CODE_DFLT = "RUB"
}

/** Опциональное поле currencyCode, подразумевающее дефолтовую валюту. */
trait CurrencyCodeOpt {
  def currencyCodeOpt : Option[String]

  def currencyCode = currencyCodeOpt getOrElse CurrencyCodeOpt.CURRENCY_CODE_DFLT
  def currency = Currency.getInstance(currencyCode)
}

