package models.maps.umap

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.16 16:49
  * Description: Тесты для модели [[FeatureCollection]].
  */
class FeatureCollectionSpec extends PlaySpec {

  "JSON" must {

    "handle real datalayer JSON from umap.js" in {

      val raw = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[30.22871017456055,59.92689354421188],[30.23428916931152,59.92779674512012],[30.23853778839111,59.92742041439657],[30.235018730163574,59.92477522657868],[30.22871017456055,59.92689354421188]]]},"properties":{"name":"ТЦ 4","description":"http://localhost:9000/sys/market/adn/JFnBBUkeSXGSt-yRtp_jZQ"}},{"type":"Feature","geometry":{"type":"Point","coordinates":[30.236692428588867,59.92665698752978]},"properties":{"name":"ТЦ 4 (Центр)","description":"http://localhost:9000/sys/market/adn/JFnBBUkeSXGSt-yRtp_jZQ"}}],"_storage":{"displayOnLoad":true,"name":"Здания","id":1}}"""

      val jsRes = Json.parse(raw)
        .validate[FeatureCollection]

      jsRes.isSuccess mustBe true

      val fc = jsRes.get
      fc.ftype mustBe FeatureTypes.FeatureCollection
    }

  }

}
