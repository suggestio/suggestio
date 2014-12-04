package util.ai.sax.currency.cbrf

import models.ai.{MAiParserCtxT, CurrenciesInfoBeanT}
import util.ai.sax.AiSaxPlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.14 16:03
 * Description: Тесты для SAX-хандлера [[CbrfCurDayXmlSax]].
 */
class CbrfCurDayXmlSaxSpec extends AiSaxPlaySpec  {

  override val RES_DIR = "/util/ai/sax/currency/cbrf/"

  override type H = CbrfCurDayXmlSax
  override type T = CurrenciesInfoBeanT

  override def getHandler(ctx: MAiParserCtxT): H = new CbrfCurDayXmlSax
  override def getResult(handler: H): T = handler.getParseResult

  "dump of daily.20140412.xml" must {
    val info = Info(
      url = "http://www.cbr.ru/scripts/XML_daily.asp",
      fileNameOpt = Some("daily.20140412.xml"),
      contentType = "text/xml"
    )
    val res = doParse(info)

    "test map" in {
      res.getMap.nonEmpty mustBe true
    }

    "EUR" in {
      res.getMap.contains("EUR") mustBe true
      val usd = res.getMap("USD")
      usd.charCode    mustBe "USD"
      usd.count       mustBe 1
      usd.course      mustBe 52.6932F
      usd.getName     mustBe "Доллар США"
      usd.numCodeOpt  mustBe Some("840")
    }

    "USD" in {
      res.getMap.contains("USD") mustBe true
      val eur = res.getEur
      eur.charCode    mustBe "EUR"
      eur.count       mustBe 1
      eur.course      mustBe 64.8443F
      eur.getName     mustBe "Евро"
      eur.numCodeOpt  mustBe Some("978")
    }

    // TODO Нужно тестировать материал от ЦБ по ссылке заново.
  }


  "Current http://www.cbr.ru/scripts/XML_daily.asp" must {
    val info = Info(
      url = "http://www.cbr.ru/scripts/XML_daily.asp",
      fileNameOpt = None,
      contentType = "text/xml"
    )
    val res = doParse(info)

    "test map" in {
      res.getMap.nonEmpty mustBe true
    }

    "EUR" in {
      val eur = res.getEur
      eur.charCode    mustBe "EUR"
      eur.count       mustBe 1
      eur.getName     mustBe "Евро"
    }

    "USD" in {
      res.getMap.contains("USD") mustBe true
      val usd = res.getUsd
      usd.charCode    mustBe "USD"
      usd.count       mustBe 1
      usd.getName     mustBe "Доллар США"
      usd.numCodeOpt  mustBe Some("840")
    }
  }

}
