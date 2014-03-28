package util

import io.suggest.ym.parsers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.14 19:25
 * Description:
 */
object UserInputParsers {

  /** Парсер для цены. Если валюта не указана, то используются рубли. */
  val priceParser = {
    import PriceParsers._
    val dfltPriceParser = floatP ^^ { price => Price(price, currRUBc) }
    currPriceParser | dfltPriceParser
  }


  /** Парсер для процентных значений. */
  val percentParser = {
    import PercentParser._
    floatP <~ opt(pcSignParser)
  }

}
