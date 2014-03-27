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
  val floatP: Parser[Float] = """-?(\d+([.,]+\d*)?|\d*[.,]+\d+)""".r ^^ { _.toFloat }

  type Price_t = (Float, Currency)
  type PriceP_t = Parser[Price_t]

  val currRUBc = Currency.getInstance("RUB")

  // Российские рубли. FIXME Белорусские рубли тоже могут сюда попасть, если обозначены через "руб." Латинская p тоже подходит.
  val priceRUBp: PriceP_t = {
    val currRUBp: Parser[_] = "(?iu)р(уб(л?[а-я]*)?.?".r | "(?i)(r(ou|u|uo)b(le?)|RUR)".r | "?(i)p\\.?".r
    (floatP <~ opt(currRUBp)) ^^ { price => price -> currRUBc }
  }

  // Доллары США
  val priceUSDp: PriceP_t = {
    val currUSDc = Currency.getInstance("USD")
    val dsign: Parser[_] = "$"
    val usd: Parser[_] = "(?i)USD".r
    val dollar: Parser[_] = "?(iu)долл?ар[а-я]{0,3}".r
    val prefixPrice = (dsign | usd) ~> floatP
    val postfixPrice = floatP <~ (dsign | usd | dollar)
    (prefixPrice | postfixPrice) ^^ { floatPrice => (floatPrice, currUSDc) }
  }

  // Евро
  val priceEURp: PriceP_t = {
    val currEURc = Currency.getInstance("EUR")
    val esign: Parser[_] = "€"
    val eur: Parser[_] = "(?i)EUR".r
    val evro: Parser[_] = "(?iu)(й?[еэ])[ву]ро".r
    val prefixPrice = (esign | eur) ~> floatP
    val postixPrice = floatP <~ (esign | eur | evro)
    (prefixPrice | postixPrice) ^^ { price => (price, currEURc) }
  }


  // Парсинг цен. Нужно разбирать, что там юзеры вдалбливают в качестве цены. Это не обязательно является числом.
  /** Парсер произвольной строки с ценой. Цена может содержать валюту, и точки-запятые в качестве разделей десятичной части. */
  val currPriceParser: PriceP_t = {
    priceRUBp | priceUSDp | priceEURp
  }

}