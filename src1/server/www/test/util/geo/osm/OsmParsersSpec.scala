package util.geo.osm

import org.scalatestplus.play._
import util.geo.osm.OsmElemTypes.OsmElemType

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.14 17:52
 * Description: Тесты для combination parsers.
 */
class OsmParsersSpec extends PlaySpec {

  "OsmParsers" must {
    val parser = new OsmParsers
    import parser._

    "parse object geo-browse URLs (String)" in {
      def t(url: String, resTyp: OsmElemType, resId: Long): Unit = {
        val pr1 = parse(osmBrowserUrl2TypeIdP, url)
        pr1.successful mustBe true
        pr1.get._1 mustBe resTyp
        pr1.get._2 mustBe resId
      }
      t("http://www.openstreetmap.org/node/2474952919#map=18/59.93284/30.25950", OsmElemTypes.NODE, 2474952919L)
      t("http://www.openstreetmap.org/way/31399147#map=18/59.93284/30.25950", OsmElemTypes.WAY, 31399147L)
      t("http://www.openstreetmap.org/relation/368287#map=11/59.8721/30.4651", OsmElemTypes.RELATION, 368287L)
    }
  }

}
