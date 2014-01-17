package io.suggest.ym

import io.suggest.ym.AnyOfferFields.AnyOfferField
import YmlSax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.14 18:38
 * Description: Исключения парсеров форматов яндекс-маркета.
 */

/** Исключение верхнего уровня для всех парсеров sio.ym.*. */
class YmParseException extends IllegalArgumentException

abstract class YmShopException extends YmParseException {
  def shopName: String
  override def getMessage: String = shopName
}

abstract class YmOfferException extends YmShopException {
  def offerIdOpt: Option[String]
  override def getMessage: String = {
    val msg0 = super.getMessage
    if (offerIdOpt.isDefined) {
      msg0 + "[" + offerIdOpt.get + "]"
    } else {
      msg0
    }
  }
}

abstract class YmOfferFieldException extends YmOfferException {
  def offerField: AnyOfferField
  def offerFieldName = offerField.toString
  def msg: String
  override def getMessage: String = super.getMessage + "." + offerFieldName + ": " + msg
}

case class OfferAgeInvalidException(shopName:String, offerIdOpt:Option[String], msg: String) extends YmOfferFieldException {
  def offerField = AnyOfferFields.age
}

case class OfferParamInvalidException(shopName:String, offerIdOpt: Option[String], msg: String) extends YmOfferFieldException {
  def offerField = AnyOfferFields.param
}

// TODO Перегнать остальные исключения куда надо.
case class ExpectedAttrNotFoundException(tag:String, attr:String)
  extends IllegalArgumentException(s"Expected attribute '$attr' not found in tag '$tag'.")

case class UndefinedCurrencyIdException(id: String, definedCurrencies: List[ShopCurrency])
  extends IllegalArgumentException(s"Unexpected currencyId: '$id'. Defined shop's currencies are: ${definedCurrencies.map(_.id).mkString(", ")}")

case class UrlTooLongException(url:String, maxLen:Int)
  extends IllegalArgumentException("URL too long. MaxLen = " + maxLen)

case class MarketCategoryTooLongException(str: String)
  extends IllegalArgumentException("Market category too long for reading: " + str)

case class ShopCategoryTooLongException(cat: String)
  extends IllegalArgumentException("Shop category name or id too long. maxLen=" + SHOP_CAT_VALUE_MAXLEN + " : " + cat)

case class CurrencyIdTooLongException(currId: String)
  extends IllegalArgumentException("Shop currency has too long id: " + currId + " ;; max str length = " + SHOP_CURRENCY_ID_MAXLEN)

