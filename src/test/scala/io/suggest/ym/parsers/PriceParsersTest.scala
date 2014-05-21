package io.suggest.ym.parsers

import org.scalatest._
import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.14 20:31
 * Description: Тесты для парсеров цен.
 */
class PriceParsersTest extends FlatSpec with Matchers {

  import PriceParsers._

  "PriceParsers" should "parse RUB prices" in {
    val rp = rpF(priceRUBp, currRUBc)
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
    val dp = rpF(priceUSDp, currUSDc)
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
    val ep = rpF(priceEURp, currEURc)
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
    * @param curr правильная валюта результата.
    * @param lag допустимая точность float.
    * @return Функция тестирования, принимающая исходную строку и результат.
    */
  private def rpF(p: PriceP_t, curr: Currency, lag: Float = 0.02F) = {
    (s: String, result: Float) =>
      val pres = parse(p, s: String)
      pres.successful shouldBe true
      val Price(price, curr) = pres.get
      curr shouldBe curr
      price shouldBe result +- 0.02F
  }
}
