package io.suggest.text.parse.price

import org.scalatest._
import org.scalatest.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.14 20:31
 * Description: Тесты для парсеров цен.
 */
class PriceParsersSpec extends FlatSpec {

  private lazy val pp = new PriceParsersImpl

  "PriceParsers" should "parse RUB prices" in {
    val rp = rpF(pp.priceRUBp, pp.RUB_CC)
    // русский
    rp("10 руб.", 10F)
    rp("10.5 руб", 10.5F)
    rp("10,5 руб", 10.5F)
    rp("1000р.", 1000F)
    rp("1000 РублеЙ", 1000F)
    // Бывают числа с пробелами через три знака, разделенные non-ASCII-пробелами.
    rp("10 000 р.", 10000F)
    rp("10 000.40 р.", 10000.40F)
    // английский
    rp("100.50 RUB", 100.50F)
    rp("1234 roubles", 1234F)
    rp("1234.5 p.", 1234.5F)
    rp("RUB100", 100F)
    rp("RUB 100", 100F)
  }

  it should "parse USD prices" in {
    val dp = rpF(pp.priceUSDp, pp.USD_CC)
    dp("$1", 1F)
    dp("$ 1", 1F)
    dp("1$", 1F)
    dp("1050 USD", 1050F)
    dp("10. $", 10F)
    dp("USD 15.40", 15.40F)
    // русский
    dp("10 долларов", 10F)
    dp("5.50 доллара", 5.50F)
    dp("5,50 доллара", 5.50F)
    dp("5 баксов", 5F)
  }

  it should "parse EUR prices" in {
    val ep = rpF(pp.priceEURp, pp.EUR_CC)
    ep("€5.40", 5.40F)
    ep("5,5€", 5.5F)
    ep("6.66 EUR", 6.66F)
    ep("6,66 EUR", 6.66F)
    ep("6 euro", 6F)
    // русский
    ep("4,6 евро", 4.6F)
    ep("4.6 еуро", 4.6F)
  }


  /** Генератор функций тестирования.
    * @param p парсер денег.
    * @param currencyCode Код правильной валюта результата.
    * @param lag допустимая точность float.
    * @return Функция тестирования, принимающая исходную строку и результат.
    */
  private def rpF(p: pp.PriceP_t, currencyCode: String, lag: Double = 0.02) = {
    (s: String, result: Float) =>
      val pres = pp.parse(p, s: String)
      pres.successful shouldBe true
      val ParsedPrice(price2, currencyCode2) = pres.get
      currencyCode2 shouldBe currencyCode
      price2 shouldBe result +- 0.02F
  }
}
