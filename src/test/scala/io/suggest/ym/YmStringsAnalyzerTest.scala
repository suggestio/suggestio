package io.suggest.ym

import org.scalatest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 31.01.14 14:00
 * Description: Тесты для анализатора-нормализатора текстов.
 */

class YmStringsAnalyzerTest extends FlatSpec with Matchers {

  "toNormTokens()" should "prepare different strings" in {
    val an = new YmStringsAnalyzer
    an.toNormTokensRev("Дома")      shouldBe List("дом")
    an.toNormTokensRev("Для дома")  shouldBe List("дом")
    an.toNormTokensRev("Хата Дома") shouldBe List("дом", "хат")
    an.toNormTokensDirect("Автомобильные видеорегистраторы ") shouldBe List("автомобильн", "видеорегистратор")
    an.toNormTokensDirect("аКсеccуаРы") shouldBe List("аксессуар")    // в слове есть неправильная c/с
  }

}
