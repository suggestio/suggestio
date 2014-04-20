package models

import play.api.mvc.Call
import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.14 17:09
 * Description: Моделе-подобное барахло, которые в основном нужно для шаблонов.
 */


/** Интерфейс контекста для рендера шаблона lk.adn.adnNodeShowTpl.
  * Шаблону надо иметь доступ к адресам редактирования, списка арендаторов и т.д. */
trait ShowAdnNodeCtx {
  def producersShowCall(adnId: String): Call
  def nodeEditCall(adnId: String): Call
  def createAdCall(adnId: String): Call
  def editAdCall(adId: String): Call
}


object CurrencyCodeOpt {
  val CURRENCY_CODE_DFLT = "RUB"
}

/** Опциональное поле currencyCode, подразумевающее дефолтовую валюту. */
trait CurrencyCodeOpt {
  def currencyCodeOpt : Option[String]

  def currencyCode = currencyCodeOpt getOrElse CurrencyCodeOpt.CURRENCY_CODE_DFLT
  def currency = Currency.getInstance(currencyCode)
}

