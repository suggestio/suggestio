package util

import scala.util.parsing.combinator.JavaTokenParsers
import io.suggest.ym.parsers.PriceParsers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.14 19:25
 * Description:
 */
object UserInputParsers extends JavaTokenParsers {

  /** Парсер для цены. Если валюта не указана, то используются рубли. */
  val priceParser: PriceP_t = {
    val dfltPriceParser = floatP ^^ { price => (price, currRUBc) }
    currPriceParser | dfltPriceParser
  }

}
