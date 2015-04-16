package models.mext.tw

import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.15 15:47
 * Description: Тесты для oauth1-поддержки твиттера.
 */
class TwOAuth1SupportSpec extends PlaySpec {

  import OAuth1Support._

  "str2tweetLeadingText" must {
    def assertStrlen(s: String): Unit = {
      s.length must be <= LEAD_TEXT_LEN
    }

    "Minify NOT-SO-LONG ad rich descr" in {
      val text =
        """
          |<br/>
          |  PR-кампании в  Москве и Санкт-Петербурге
          |<br>
          |test <strong>test</strong>    test
          |
        """.stripMargin
      str2tweetLeadingText(text) mustBe """PR-кампании в Москве и Санкт-Петербурге test test test"""
    }

    "Minify very long rich.descr" in {
      val text =
        """
          |<p> <font>Очень много букв.</font> </p>
          |
          |
          |
          |<p>Очень много букв.Очень много букв.Очень много букв.Очень много букв.</p>
          |<br/>
          |<p>Очень много букв.Очень много букв.</p>
          |
          |Очень много букв.
          |
          |<p>Очень много букв.</p>
        """.stripMargin
      // Для само-контроля контроллируем длину заданного правильного результата. Если первы тест тютю, то значит что-то изменилось слишком сильно.
      val resText = """Очень много букв. Очень много букв.Очень много букв.Очень много букв.Очень много букв. Очень много букв.Очень много…"""
      assertStrlen(resText)
      str2tweetLeadingText(text) mustBe resText
    }

    "Minify other long rich.descr" in {
      val text =
        """
          |<p style="text-align: center;"><span style="font-size: 26px; color: #b61010;">ZagolovОК</span>&nbsp;</p><p>&nbsp;</p><p>Опейсание содержит <strong>много букв</strong>. Это помогает донести суть до всех. Форматирование дОлЖнО быть <span style="font-size: 22px;">очень</span> суровым, <span style="font-size: 12px;">чтобы донести</span> суть до всех.</p><p>&nbsp;</p><p>&nbsp;</p><p>asdf</p><p>as awe<span style="background-color: #00ff68;">fa</span>wef</p><p>f</p><p>asd&nbsp;</p><p>f</p><p>asdfasdfawef awef awefawef awef</p><p>&nbsp;</p><p>&nbsp;</p>
        """.stripMargin
      val res = str2tweetLeadingText(text)
      assertStrlen(res)
    }

  }

}
