package models.msys

import util.geo.osm.OsmElemTypes.OsmElemType
import util.geo.osm.OsmParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.06.15 18:53
 * Description: Модель представления результатов парсинга URL на объект (узел/путь/отношение) на API osm.org.
 */

object OsmUrlParseResult {
  def fromUrl(url: String): Option[OsmUrlParseResult] = {
    val parser = new OsmParsers
    val pr = parser.parse(parser.osmBrowserUrl2TypeIdP, url)
    if (pr.successful) {
      Some(OsmUrlParseResult(
        url = url,
        osmType = pr.get._1,
        id = pr.get._2
      ))
    } else {
      None
    }
  }
}

case class OsmUrlParseResult(url: String, osmType: OsmElemType, id: Long)
