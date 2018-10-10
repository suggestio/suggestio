package util

import io.suggest.bill.{Amount_t, MCurrencies, MPrice}
import io.suggest.common.html.HtmlConstants
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
    def ttt: String = HtmlConstants.ELLIPSIS

    "keep short strings" in {
      t("asdasd")           mustBe "asdasd"
      t("")                 mustBe ""
      t("1234567890")       mustBe "1234567890"
    }

    "keep no-so-long strings without ... at EOL" in {
      t("asdasdasdasd")     mustBe "asdasdasdasd"
    }

    s"strip long strings with $ttt at EOL" in {
      t("Aasdasd asdasd restRest") mustBe s"Aasdasd asdasd$ttt"
    }

    s"strip long multi-line texts with $ttt EOL" in {
      val text = longText
      t(text) mustBe s"РАЗРАБОТКА$ttt"
      t(text, 50).length mustBe 50 +- 11
      t(text, 50, hard = true).length must be <= 50
    }

    "hardly strip very long texts #1" in {
      val l = 118
      val text = """РАЗРАБОТКА И ПРОВЕДЕНИЕ PR-КАМПАНИЙ В МОСКВЕ Планирование и проведение PR-кампаний в Москве – один из наиболее востребованных сегментов деятельности PR-агентства  C | B | C | A"""
      val res = t(text, l, hard = true)
      res.length must be <= l
    }

    "hardly strip very long texts #2" in {
      val l = 117
      val text = """РАЗРАБОТКА И ПРОВЕДЕНИЕ PR-КАМПАНИЙ В МОСКВЕ Планирование и проведение PR-кампаний в Москве – один из наиболее востребованных сегментов деятельности PR-агентства  C | B | C | A"""
      val res = t(text, l, hard = true)
      res.length must be < l
    }


    "hardly strip texts, that where ellipsis must on len+1 position" in {
      val l = 10
      val text = """РАЗРАБОТКА И ПРОВЕДЕНИЕ"""
      val res = t(text, l, hard = true)
      res.length must be <= l
    }

  }


  "formatPriceAmountPlain()" must {

    import TplDataFormatUtil.{formatPriceAmountPlain => f}
    def t(amount: Amount_t) = f( MPrice(amount, MCurrencies.default) )

    "zero price" in {
      t(0L) mustBe "0.00"
    }
    "1 RUB => 1.00" in {
      t(1L) mustBe "1.00"
    }
    "100500 RUB => 100500.00" in {
      t(100500L) mustBe "100500.00"
    }
    "100 500 100 500.10 RUB => 100500100500.10" in {
      t(100500100500L) mustBe "100500100500.10"
    }
    "100 500 100 500.001 RUB => 100500100500.00" in {
      t(100500100500L) mustBe "100500100500.00"
    }

  }

}
