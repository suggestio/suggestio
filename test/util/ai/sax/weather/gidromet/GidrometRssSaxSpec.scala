package util.ai.sax.weather.gidromet

import functional.WithInputStream
import models.ai._
import org.apache.tika.metadata.{HttpHeaders, Metadata}
import org.apache.tika.metadata.TikaMetadataKeys.RESOURCE_NAME_KEY
import org.apache.tika.parser.html.{IdentityHtmlMapper, HtmlMapper}
import org.apache.tika.parser.{ParseContext, AutoDetectParser}
import org.joda.time.LocalDate
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.14 15:16
 * Description: Тесты для RSS-SAX-парсера rss-прогнозов росгидромета.
 * Тут имитируется вызов парсера через tika.
 */
class GidrometRssSaxSpec extends PlaySpec with WithInputStream {

  override val RES_DIR = "/util/ai/sax/weather/gidromet/"

  /** Инфа по парсингу одного экземпляра готового rss. */
  protected case class Rss(url: String, fileName: String, contentType: String)

  protected def tika(rss: Rss) = {
    // Сборка цепочки парсинга
    val saxHandler = new GidrometRssSax
    val parser = new AutoDetectParser()   // TODO использовать HtmlParser? Да, если безопасно ли скармливать на вход HtmlParser'у левые данные.
    val parseContext = new ParseContext
    parseContext.set(classOf[HtmlMapper], new IdentityHtmlMapper)
    val meta = new Metadata
    meta.add(RESOURCE_NAME_KEY, rss.url)
    meta.add(HttpHeaders.CONTENT_TYPE, rss.contentType)  // гидромет раздаёт неправильный content-type.
    withFileStream(rss.fileName) { is =>
      parser.parse(is, saxHandler, meta)
    }
    saxHandler.accRev
  }

  "GidrometRssSax parser" must {
    "handle SPb rss weather (26063-20141201.rss) via tika" in {
      val rss = Rss(
        url = "http://meteoinfo.ru/rss/forecasts/26063",
        fileName = "26063-20141201.rss",
        contentType = "Content-Type: text/html; charset=UTF-8"
      )
      val res = tika(rss)
      res.size              mustBe 3
      val d3 = res.head   // d3 = december 3
      d3.date               mustBe new LocalDate(2014, 12, 3)
      d3.precipations       mustBe List(Precipations.NoPercipations)
      d3.precipChanceOpt    mustBe Some(41)
      d3.pressureMmHg       mustBe AtmPressure(Some(761), nightOpt = Some(764))
      d3.skyStateOpt        mustBe Some(SkyStates.CloudVary)
      d3.temperatures       mustBe Temperatures(Some(2F), nightOpt = Some(-4))
      d3.windOpt            mustBe Some(Wind(GeoDirections.WEST, 6))
    }
  }

}
