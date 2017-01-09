package io.suggest.ym.parsers

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.matching.Regex

// TODO 12 feb 2016 Выпихнуть цены и утиль для них в отдельный пакет вместе с моделью mbill2 MPrice.
// Рабочие form mapping'и для цен были удалены из web21:FormUtil после 8e3432fbf693

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.14 19:20
 * Description: Парсер цен из строк.
 */

trait PriceParsers extends JavaTokenParsers with CommonParsers {

  type PriceP_t = Parser[ParsedPrice]

  override protected val whiteSpace: Regex = "(?U)\\s+".r

  def RUB_CC = "RUB"
  def USD_CC = "USD"
  def EUR_CC = "EUR"

  /** Парсер цен в российских рублях. */
  def priceRUBp: PriceP_t = {
    val rubl: Parser[_] = "(?iu)[рp]([уy][б6](л[а-я]*)?)?\\.?".r
    val rub: Parser[_] = "(?i)RU[BR]".r
    val rouble = "(?i)r(ou|u|uo)b(le?s?)?|".r | rub
    val currRUBp = rubl | rouble
    // Добавляем обработку grouping-разделителей внутрь float, чтобы обрабатывать числа вида "10 000".
    val floatGroupedRe = """(?U)-?(\d[\d\s]*([.,]+\d*)?|\d*[.,]+\d+)""".r
    val floatGroupedP: Parser[Float] = floatGroupedRe ^^ { rawFloatStr =>
      val floatStr = rawFloatStr.replaceAll("(?U)\\s+", "")
      ParserUtil.str2Float(floatStr)
    }
    val postfixPrice = floatGroupedP <~ currRUBp
    val prefixPrice = rub ~> floatGroupedP
    (postfixPrice | prefixPrice) ^^ { price => ParsedPrice(price, RUB_CC) }
  }


  /** Парсер цен в долларах США. */
  def priceUSDp: PriceP_t = {
    val dsign: Parser[_] = "$"
    val usd: Parser[_] = "(?i)USD".r
    val dollar: Parser[_] = "(?iu)(д[оo]лл?[аaоo][рpr]|[б6][aа][kк]+[cс])[а-я]{0,3}".r
    // TODO Вместо floatP надо задействовать парсер американских цифр вида "123,456,768.23".
    val _doubleP = doubleP
    val prefixPrice = (dsign | usd) ~> _doubleP
    val postfixPrice = _doubleP <~ (dsign | usd | dollar)
    (prefixPrice | postfixPrice) ^^ { floatPrice => ParsedPrice(floatPrice, USD_CC) }
  }


  /** Парсер цен в евро. */
  def priceEURp: PriceP_t = {
    val esign: Parser[_] = "€"
    val eur: Parser[_] = "(?i)EURo?".r
    val evro: Parser[_] = "(?iu)(й?[еэe])[вуuv][рr][оo]".r
    val _doubleP = doubleP
    val prefixPrice = (eur | esign) ~> _doubleP
    val postixPrice = _doubleP <~ (eur | evro | esign)
    (prefixPrice | postixPrice) ^^ { price => ParsedPrice(price, EUR_CC) }
  }


  /** Парсер произвольной строки с ценой в России.
    * Цена должна содержать валюту, и точки-запятые в качестве разделей десятичной части. */
  def currPriceParser: PriceP_t = {
    priceRUBp | priceUSDp | priceEURp
  }


}

/** Дефолтовая реализация [[PriceParsers]]. */
class PriceParsersImpl extends PriceParsers

/** Модель распарсенной цена с указанием кода валюты. */
case class ParsedPrice(price: Float, currencyCode: String)

