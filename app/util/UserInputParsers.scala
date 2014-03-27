package util

import scala.util.parsing.combinator.JavaTokenParsers
import io.suggest.ym.parsers.PriceParsers._
import io.suggest.ym.parsers.Price

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.14 19:25
 * Description:
 */
object UserInputParsers extends JavaTokenParsers {

  /** Парсер для цены. Если валюта не указана, то используются рубли. */
  val priceParser = {
    val dfltPriceParser = floatP ^^ { price => Price(price, currRUBc) }
    currPriceParser | dfltPriceParser
  }

}
