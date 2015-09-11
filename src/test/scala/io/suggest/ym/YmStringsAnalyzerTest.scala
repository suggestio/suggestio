package io.suggest.ym

import org.scalatest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 31.01.14 14:00
 * Description: Тесты для анализатора-нормализатора текстов.
 */

class YmStringsAnalyzerTest extends FlatSpec with Matchers {

  private def _an = new YmStringsAnalyzer

  "toNormTokens()" should "reverse steam simple examples" in {
    val an = _an
    an.toNormTokensRev("Дома")      shouldBe List("дом")
    an.toNormTokensRev("Для дома")  shouldBe List("дом")
    an.toNormTokensRev("Хата Дома") shouldBe List("дом", "хат")
  }

  it should "direct stream simple examples" in {
    _an.toNormTokensDirect("Автомобильные видеорегистраторы ") shouldBe List("автомобильн", "видеорегистратор")
  }

  it should "prepare different strings" in {
    _an.toNormTokensDirect("аКсеccуаРы") shouldBe List("аксессуар")    // в слове есть неправильная c/с
  }

  it should "handle more complex strings" in {
    val an = _an
    // TODO Тут идёт потеря смысла исходника: отрицательный температура стала положительной. Это наверное надо исправлять.
    an.toNormTokensDirect("Температура ночью -6°, днём -2°.") shouldBe List("температур", "ноч", "6", "днём", "2")
    an.toNormTokensDirect("Санкт-Петербург, 30 ноября") shouldBe List("санкт", "петербург", "30", "ноябр")
  }

}
