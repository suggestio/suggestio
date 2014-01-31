package io.suggest.ym

import org.xml.sax.{SAXParseException, Locator}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.14 18:38
 * Description: Исключения парсеров форматов яндекс-маркета.
 * В экзепшенах избегаем какого-либо хранения рантаймовых значений и переменных.
 */

// TODO Надо бы наследовать SAXParseException, который принимает на вход локатор. Вероятно, некоторые куски тут надо
// на java написать, т.к. переданный locator нельзя сохранять в полях класса, а scala-классы делают это постоянно.

/** Исключение верхнего уровня для всех парсеров sio.ym.*. */
abstract class YmParserException extends Exception {
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

/** Какой-то экзепшен во внешнем SAX-парсере. */
class YmSaxException(cause: SAXParseException) extends YmParserException {
  def lineNumber = cause.getLineNumber
  def columnNumber = cause.getColumnNumber
  override def getCause = cause
  override def getMessageBuilder: StringBuilder = super.getMessageBuilder.append(cause.getMessage)
}


/** Исключение, которое ни к какой конкретной категории исключений не относится. */
case class YmOtherException(lineNumber:Int, columnNumber:Int, msg:String, cause:Throwable) extends YmParserException {
  override def getMessageBuilder: StringBuilder = super.getMessageBuilder.append(msg)
  override def getCause = if (cause != null) cause else this
}
object YmOtherException {
  def apply(msg: String, cause:Throwable = null)(implicit locator:Locator): YmOtherException = {
    YmOtherException(
      lineNumber = locator.getLineNumber,
      columnNumber = locator.getColumnNumber,
      msg = msg,
      cause = cause
    )
  }
}


abstract class YmShopException extends YmParserException {
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
  extends YmShopException {
  override def getMessageBuilder: StringBuilder = super.getMessageBuilder.append(msg)
}


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
  def apply(attr:String, msg:String, cause: Throwable = null)(implicit shop:ShopHandlerState, offer:OfferHandlerState, locator:Locator) = {
    YmOfferFieldException(
      lineNumber = locator.getLineNumber,
      columnNumber = locator.getColumnNumber,
      shopName = if (shop == null) null else shop.name,
      offerIdOpt = offer.idOpt,
      fn = attr,
      msg = msg,
      cause = cause
    )
  }
}
object YmOfferFieldException {
  def apply(msg: String, cause: Throwable = null)(implicit shop:ShopHandlerState, offer:OfferHandlerState, locator:Locator, fh:SimpleValueT): YmOfferException = {
    YmOfferFieldException(
      lineNumber = locator.getLineNumber,
      columnNumber = locator.getColumnNumber,
      shopName = if (shop == null) null else shop.name,
      offerIdOpt = offer.idOpt,
      fn = fh.myTag,
      msg = msg,
      cause = cause
    )
  }
}
case class YmOfferFieldException(lineNumber:Int, columnNumber:Int, shopName:String, offerIdOpt: Option[String], fn:String, msg:String, cause:Throwable)
  extends YmOfferException {
  override def getMessageBuilder = super.getMessageBuilder.append('.').append(fn).append(": ").append(msg)
}

