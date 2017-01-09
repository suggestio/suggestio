package util.ai.sax.weather.gidromet

import models.ai._
import org.joda.time.LocalDate
import util.ai.sax.AiSaxPlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.14 15:16
 * Description: Тесты для RSS-SAX-парсера rss-прогнозов росгидромета.
 */
class GidrometRssSaxSpec extends AiSaxPlaySpec {

  override val RES_DIR = "/util/ai/sax/weather/gidromet/"

  override type H = GidrometRssSax
  override type T = List[DayWeatherBean]

  override def getHandler(ctx: MAiParserCtxT): H = new GidrometRssSax(ctx)
  override def getResult(handler: H): T = handler.accRev


  "SPb 26063-20141201.rss dump" must {
    "parse SPb rss weather via default parser" in {
      val rss = Info(
        url = "http://meteoinfo.ru/rss/forecasts/26063",
        fileNameOpt = Some("26063-20141201.rss"),
        contentType = "text/html; charset=UTF-8"
      )
      val res = doParse(rss)
      res.size              mustBe 3
      val d3 = res.head   // d3 = december 3
      // В дате прогноза год только подразумевается. Поэтому надо по-лучше потестить прогнозы на стыке годов дек-янв.
      d3.date.getMonthOfYear mustBe 12
      d3.date.getDayOfMonth  mustBe 3
      //d3.date             mustBe new LocalDate(2014, 12, 3)
      d3.precipations       mustBe List(Precipations.NoPercipations)
      d3.precipChanceOpt    mustBe Some(41)
      d3.pressureMmHg       mustBe AtmPressure(Some(761), nightOpt = Some(764))
      d3.skyStateOpt        mustBe Some(SkyStates.CloudVary)
      d3.temperatures       mustBe Temperatures(Some(2F), nightOpt = Some(-4))
      d3.windOpt            mustBe Some(Wind(GeoDirections.WEST, 6))
    }
  }

  "SPb http://meteoinfo.ru/rss/forecasts/26063" must {
    "parse current SPb weather via default parser" in {
      val rss = Info(
        url = "http://meteoinfo.ru/rss/forecasts/26063",
        fileNameOpt = Some("26063-20141201.rss"),
        contentType = "text/html; charset=UTF-8"
      )
      val res = doParse(rss)
      assert( res.size > 2 )
    }
  }

}
