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

  "rdescr2tweetLeadingText" must {
    def assertStrlen(s: String): Unit = {
      s.length must be <= LEAD_TEXT_LEN
    }

    "minify NOT-SO-LONG ad rich descr" in {
      val text =
        """
          |<br/>
          |  PR-кампании в  Москве и Санкт-Петербурге
          |<br>
          |test <strong>test</strong>    test
          |
        """.stripMargin
      rdescr2tweetLeadingText(text) mustBe """PR-кампании в Москве и Санкт-Петербурге test test test"""
    }

    "minify very long rich.descr" in {
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
      rdescr2tweetLeadingText(text) mustBe resText
    }

    "minify other long rich.descr" in {
      val text =
        """
          |<p style="text-align: center;"><span style="font-size: 26px; color: #b61010;">ZagolovОК</span>&nbsp;</p><p>&nbsp;</p><p>Опейсание содержит <strong>много букв</strong>. Это помогает донести суть до всех. Форматирование дОлЖнО быть <span style="font-size: 22px;">очень</span> суровым, <span style="font-size: 12px;">чтобы донести</span> суть до всех.</p><p>&nbsp;</p><p>&nbsp;</p><p>asdf</p><p>as awe<span style="background-color: #00ff68;">fa</span>wef</p><p>f</p><p>asd&nbsp;</p><p>f</p><p>asdfasdfawef awef awefawef awef</p><p>&nbsp;</p><p>&nbsp;</p>
        """.stripMargin
      val res = rdescr2tweetLeadingText(text)
      assertStrlen(res)
    }

    "minify very-very real long rich.descr" in {
      val text = "<div style=\"text-align: left;\">\r\n<p style=\"text-align: center;\"><span style=\"color: #000000;\"><strong><span style=\"font-size: 18px; font-family: futurfutc-webfont;\">РАЗРАБОТКА И ПРОВЕДЕНИЕ </span> </strong> </span> </p>\r\n<p style=\"text-align: center;\"> </p>\r\n<p style=\"text-align: center;\"><span style=\"color: #ffffff;\"><strong><span style=\"font-size: 26px; font-family: futurfutc-webfont;\">PR-КАМПАНИЙ В МОСКВЕ</span> </strong> </span> </p>\r\n<p><span style=\"color: #000000;\"> </span> </p>\r\n<p><span style=\"color: #000000;\"><span style=\"font-family: futurfutc-webfont; font-size: 18px;\"> </span> </span> </p>\r\n<p style=\"text-align: center;\"><span style=\"font-size: 22px; font-family: futurfutc-webfont; color: #000000;\">Планирование и проведение <strong> </strong><strong>PR-кампаний в Москве</strong>  – <span style=\"font-size: 16px;\">один из наиболее востребованных сегментов деятельности </span> </span> </p>\r\n<p style=\"text-align: center;\"><span style=\"font-size: 22px; font-family: futurfutc-webfont; color: #000000;\"><strong>PR-агентства  C | B | C | A</strong> </span> </p>\r\n<p style=\"text-align: center;\"> </p>\r\n<p> </p>\r\n<p> </p>\r\n<p><span style=\"font-family: futurfutc-webfont; font-size: 22px; color: #000000;\">В <strong>Москве</strong> сосредоточены лучшие креативные и технологические ресурсы нашего агентства.</span> </p>\r\n<p><span style=\"font-family: futurfutc-webfont; font-size: 22px; color: #000000;\"> </span> </p>\r\n<p> </p>\r\n<p><span style=\"font-family: futurfutc-webfont; font-size: 22px; color: #000000;\"><strong>Москва </strong>– главные деловые ворота России, через которые в нашу страну приходят иностранные кампании. </span> </p>\r\n<p><span style=\"font-family: futurfutc-webfont; font-size: 22px; color: #000000;\">Новым брендам требуется эффективная <strong>PR- кампания</strong> </span> </p>\r\n<p><span style=\"font-family: futurfutc-webfont; font-size: 22px; color: #000000;\">на российском рынке. </span> </p>\r\n<p> </p>\r\n<p><span style=\"font-family: futurfutc-webfont; font-size: 22px; color: #000000;\"><span style=\"color: #ffffff;\"><strong>PR-агентство  C | B | C | A</strong> - член европейской Ассоциации рекламных агентств.</span> Это еще одна причина, по которой зарубежный бизнес выбирает <strong>C | B | C | A </strong>для разработки и проведения <strong>PR-кампании в Москве</strong>, а в дальнейшем и в России.</span> </p>\r\n<p> </p>\r\n<p> </p>\r\n<p> </p>\r\n<p style=\"text-align: center;\"> </p>\r\n<p style=\"text-align: center;\"><a href=\"http://cbca.ru/\" target=\"_blank\" rel=\"nofollow\"><span style=\"color: #ffffff;\">C | B | C | A</span></a> </p>\r\n<p style=\"text-align: center;\"> </p>\r\n<p style=\"text-align: center;\"> </p>\r\n<p style=\"text-align: center;\"> </p>\r\n<p style=\"text-align: center;\"> </p>\r\n</div>"
      val res = rdescr2tweetLeadingText(text)
      assertStrlen(res)
    }

  }

}
