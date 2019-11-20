package io.suggest.geo

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 15:18
  * Description: Тесты для модели гео-шейпов [[EnvelopeGsJvm]].
  */
class EnvelopeGsSpec extends AnyFlatSpec {

  private def fmt =
    IGeoShape.JsonFormats.allStoragesEsFormatter.envelope

  "play.json DATA_FORMAT" should "parse data from documented ES example" in {
    val jsonStr =
      """
        |{
        |  "type" : "envelope",
        |  "coordinates" : [[-77.03653, 38.897676], [-67.03653, 48.897676]]
        |}
      """.stripMargin
    val jsr = Json.parse(jsonStr).validate(fmt)
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
    val jsr = Json.toJson(gs)(fmt)
      .validate(fmt)
    jsr.isSuccess shouldBe true
    jsr.get shouldBe gs
  }

}
