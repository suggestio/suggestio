package io.suggest.model.geo

import io.suggest.geo.MGeoPoint
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 15:18
  * Description: Тесты для модели гео-шейпов [[EnvelopeGs]].
  */
class EnvelopeGsSpec extends FlatSpec {

  "play.json DATA_FORMAT" should "parse data from documented ES example" in {
    val jsonStr =
      """
        |{
        |  "type" : "envelope",
        |  "coordinates" : [[-77.03653, 38.897676], [-67.03653, 48.897676]]
        |}
      """.stripMargin
    val jsr = Json.parse(jsonStr)
      .validate(EnvelopeGs.DATA_FORMAT)
    assert(jsr.isSuccess, jsr)
    val pgs = jsr.get
    pgs shouldBe EnvelopeGs(
      topLeft     = MGeoPoint(lon = -77.03653, lat = 38.897676),
      bottomRight = MGeoPoint(lon = -67.03653, lat = 48.897676)
    )
  }

  it should "serialize-deserialize" in {
    val gs = EnvelopeGs(
      topLeft     = MGeoPoint(lon = -77.03653, lat = 38.897676),
      bottomRight = MGeoPoint(lon = -67.03653, lat = 48.897676)
    )
    val fmt = EnvelopeGs.DATA_FORMAT
    val jsr = Json.toJson(gs)(fmt)
      .validate(fmt)
    jsr.isSuccess shouldBe true
    jsr.get shouldBe gs
  }

}
