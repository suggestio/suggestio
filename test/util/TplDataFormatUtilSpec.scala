package util

import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.02.15 11:11
 * Description: Тесты для утилит форматирования текста.
 */
class TplDataFormatUtilSpec extends PlaySpec {

  "strLimitLenNoTrailingWordPart" must {
    def t: String => String = TplDataFormatUtil.strLimitLenNoTrailingWordPart(_, 10)
    def ttt: String = TplDataFormatUtil.ELLIPSIS

    "keep short strings" in {
      t("asdasd")           mustBe "asdasd"
      t("")                 mustBe ""
      t("1234567890")       mustBe "1234567890"
    }

    "Keep no-so-long strings without ... at EOL" in {
      t("asdasdasdasd")     mustBe "asdasdasdasd"
    }

    "Strip long strings with ... at EOL" in {
      t("Aasdasd asdasd restRest") mustBe s"Aasdasd asdasd$ttt"
    }
  }

}
