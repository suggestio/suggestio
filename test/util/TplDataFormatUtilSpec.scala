package util

import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.02.15 11:11
 * Description: Тесты для утилит форматирования текста.
 */
class TplDataFormatUtilSpec extends PlaySpec {

  private def longText =
    """|РАЗРАБОТКА И ПРОВЕДЕНИЕ
     |
     |PR-КАМПАНИЙ В МОСКВЕ
     |
     |Планирование и проведение  PR-кампаний в Москве  – один из наиболее востребованных сегментов деятельности
     |
     |PR-агентства  C | B | C | A
     |
     |
     |В Москве сосредоточены лучшие креативные и технологические ресурсы нашего агентства.
     |Москва – главные деловые ворота России, через которые в нашу страну приходят иностранные кампании.
     |Новым брендам требуется эффективная PR- кампания
     |на российском рынке.
     |
     |PR-агентство  C | B | C | A - член европейской Ассоциации рекламных агентств. Это еще одна причина, по которой зарубежный бизнес выбирает C | B | C | A для разработки и проведения PR-кампании в Москве, а в дальнейшем и в России.
     |
     |C | B | C | A
   """.stripMargin


  "strLimitLenNoTrailingWordPart" must {
    def t(x: String, l: Int = 10, hard: Boolean = false): String = {
      TplDataFormatUtil.strLimitLenNoTrailingWordPart(x, l, hard)
    }
    def ttt: String = TplDataFormatUtil.ELLIPSIS

    "keep short strings" in {
      t("asdasd")           mustBe "asdasd"
      t("")                 mustBe ""
      t("1234567890")       mustBe "1234567890"
    }

    "Keep no-so-long strings without ... at EOL" in {
      t("asdasdasdasd")     mustBe "asdasdasdasd"
    }

    s"Strip long strings with $ttt at EOL" in {
      t("Aasdasd asdasd restRest") mustBe s"Aasdasd asdasd$ttt"
    }

    s"Strip long multi-line texts with $ttt EOL" in {
      val text = longText
      t(text) mustBe s"РАЗРАБОТКА$ttt"
      t(text, 50).length mustBe 50 +- 11
      t(text, 50, hard = true).length must be <= 50
    }

  }

}
