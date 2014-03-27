package io.suggest.ym.parsers

import scala.util.parsing.combinator.JavaTokenParsers
import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.14 19:20
 * Description: Парсер цен из строк.
 */

object PriceParsers extends JavaTokenParsers {

  /** Парсер float-чисел, вводимых юзерами. Не исключаем, что юзеры могут вводить float-числа, начиная их с запятой. */
  val floatP: Parser[Float] = """-?(\d+([.,]+\d*)?|\d*[.,]+\d+)""".r ^^ {
    _.replace(',', '.').toFloat
  }

  type PriceP_t = Parser[Price]

  // TODO Надо бы по-сильнее отделить валюты от парсеров цен.
  val currRUBc = Currency.getInstance("RUB")

  /** Парсер цен в российских рублях. */
  val priceRUBp: PriceP_t = {
    val rubl: Parser[_]   = "(?iu)[рp]([уy][б6](л[а-я]*)?)?\\.?".r
    val rouble: Parser[_] = "(?i)(r(ou|u|uo)b(le?s?)?|RUR)".r
    val currRUBp: Parser[_] = rubl | rouble
    (floatP <~ currRUBp) ^^ { price => Price(price, currRUBc) }
  }


  val currUSDc = Currency.getInstance("USD")

  /** Парсер цен в долларах США. */
  val priceUSDp: PriceP_t = {
    val dsign: Parser[_] = "$"
    val usd: Parser[_] = "(?i)USD".r
    val dollar: Parser[_] = "(?iu)(д[оo]лл?[аaоo][рpr]|[б6][aа][kк]+[cс])[а-я]{0,3}".r
    val prefixPrice = (dsign | usd) ~> floatP
    val postfixPrice = floatP <~ (dsign | usd | dollar)
    (prefixPrice | postfixPrice) ^^ { floatPrice => Price(floatPrice, currUSDc) }
  }


  val currEURc = Currency.getInstance("EUR")

  /** Парсер цен в евро. */
  val priceEURp: PriceP_t = {
    val esign: Parser[_] = "€"
    val eur: Parser[_] = "(?i)EURo?".r
    val evro: Parser[_] = "(?iu)(й?[еэe])[вуuv][рr][оo]".r
    val prefixPrice = (eur | esign) ~> floatP
    val postixPrice = floatP <~ (eur | evro | esign)
    (prefixPrice | postixPrice) ^^ { price => Price(price, currEURc) }
  }


  /** Парсер произвольной строки с ценой в России.
    * Цена должна содержать валюту, и точки-запятые в качестве разделей десятичной части. */
  val currPriceParser: PriceP_t = {
    priceRUBp | priceUSDp | priceEURp
  }


  /** Неявный конвертер результата работы парсера в Option[T]. */
  implicit def parseResult2Option[T](pr: ParseResult[T]): Option[T] = {
    if (pr.successful)
      Some(pr.get)
    else
      None
  }

}

case class Price(price: Float, currency: Currency = PriceParsers.currRUBc)

