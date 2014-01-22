package io.suggest.ym

import org.xml.sax.Locator

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.14 18:38
 * Description: Исключения парсеров форматов яндекс-маркета.
 * В экзепшенах избегаем какого-либо хранения рантаймовых значений и переменных.
 */

/** Исключение верхнего уровня для всех парсеров sio.ym.*. */
abstract class YmParseException extends IllegalArgumentException {
  def lineNumber: Int
  def columnNumber: Int
  def getMessageBuilder: StringBuilder = {
    new StringBuilder()
      .append('(')
      .append(lineNumber)
      .append(';')
      .append(columnNumber)
      .append(") ")
  }
  override final lazy val getMessage: String = getMessageBuilder.toString()
}


abstract class YmShopException extends YmParseException {
  def shopName: String
  override def getMessageBuilder: StringBuilder = {
    val sb = super.getMessageBuilder
    if (shopName != null)
      sb.append(shopName).append(' ')
    sb
  }
}


object YmShopFieldException {
  def apply(msg: String)(implicit shop:ShopHandlerState, locator:Locator, fh:SimpleValueT): YmShopFieldException = {
    YmShopFieldException(
      lineNumber = locator.getLineNumber,
      columnNumber = locator.getColumnNumber,
      shopName = shop.name,
      fn = fh.myTag,
      msg = msg
    )
  }
}
case class YmShopFieldException(lineNumber:Int, columnNumber:Int, shopName:String, fn:String, msg:String)
  extends YmShopException


abstract class YmOfferException extends YmShopException {
  def offerIdOpt: Option[String]
  override def getMessageBuilder = {
    val sb0 = super.getMessageBuilder
    if (offerIdOpt.isDefined)
      sb0.append('[').append(offerIdOpt.get).append(']')
    else
      sb0
  }
}


object YmOfferAttributeException {
  def apply(attr:String, msg:String)(implicit shop:ShopHandlerState, offer:OfferHandlerState, locator:Locator) = {
    YmOfferFieldException(
      lineNumber = locator.getLineNumber,
      columnNumber = locator.getColumnNumber,
      shopName = if (shop == null) null else shop.name,
      offerIdOpt = offer.idOpt,
      fn = attr,
      msg = msg
    )
  }
}
object YmOfferFieldException {
  def apply(msg: String)(implicit shop:ShopHandlerState, offer:OfferHandlerState, locator:Locator, fh:SimpleValueT): YmOfferException = {
    YmOfferFieldException(
      lineNumber = locator.getLineNumber,
      columnNumber = locator.getColumnNumber,
      shopName = if (shop == null) null else shop.name,
      offerIdOpt = offer.idOpt,
      fn = fh.myTag,
      msg = msg
    )
  }
}
case class YmOfferFieldException(lineNumber:Int, columnNumber:Int, shopName:String, offerIdOpt: Option[String], fn:String, msg:String)
  extends YmOfferException {

  override def getMessageBuilder = super.getMessageBuilder.append('.').append(fn).append(": ").append(msg)
}

