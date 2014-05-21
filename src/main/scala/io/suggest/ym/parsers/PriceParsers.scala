package io.suggest.ym.parsers

import scala.util.parsing.combinator.JavaTokenParsers
import java.util.Currency
import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.14 19:20
 * Description: Парсер цен из строк.
 */

object PriceParsers extends JavaTokenParsers with CommonParsers {

  type PriceP_t = Parser[Price]

  override protected val whiteSpace: Regex = "(?U)\\s+".r

  // TODO Надо бы по-сильнее отделить валюты от парсеров цен.
  val currRUBc = Currency.getInstance("RUB")

  /** Парсер цен в российских рублях. */
  val priceRUBp: PriceP_t = {
    val rubl: Parser[_] = "(?iu)[рp]([уy][б6](л[а-я]*)?)?\\.?".r
    val rub: Parser[_] = "(?i)RU[BR]".r
    val rouble = "(?i)r(ou|u|uo)b(le?s?)?|".r | rub
    val currRUBp = rubl | rouble
    // Добавляем обработку grouping-разделителей внутрь float, чтобы обрабатывать числа вида "10 000".
    val floatGroupedRe = """(?U)-?(\d[\d\s]*([.,]+\d*)?|\d*[.,]+\d+)""".r
    val floatGroupedP: Parser[Float] = floatGroupedRe ^^ { rawFloatStr =>
      val floatStr = rawFloatStr.replaceAll("(?U)\\s+", "")
      ParserUtil.str2FloatF(floatStr)
    }
    val postfixPrice = floatGroupedP <~ currRUBp
    val prefixPrice = rub ~> floatGroupedP
    (postfixPrice | prefixPrice) ^^ { price => Price(price, currRUBc) }
  }


  val currUSDc = Currency.getInstance("USD")

  /** Парсер цен в долларах США. */
  val priceUSDp: PriceP_t = {
    val dsign: Parser[_] = "$"
    val usd: Parser[_] = "(?i)USD".r
    val dollar: Parser[_] = "(?iu)(д[оo]лл?[аaоo][рpr]|[б6][aа][kк]+[cс])[а-я]{0,3}".r
    // TODO Вместо floatP надо задействовать парсер американских цифр вида "123,456,768.23".
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


}

case class Price(price: Float, currency: Currency = PriceParsers.currRUBc)

