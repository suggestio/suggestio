package io.suggest.swfs.client.proto.lookup

import io.suggest.PlayJsonTesting
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 14:48
 * Description: Тесты для JSON-модели [[LookupResponse]].
 */
class LookupResponseSpec extends FlatSpec with PlayJsonTesting {

  "JSON" should "support empty model" in {
    _jsonTest( LookupResponse(5, Nil) )
  }

  it should "support full-filled model" in {
    _jsonTest {
      LookupResponse(
        volumeId = 3,
        locations = Seq(
          VolumeLocation("localhost:8080", url = "127.0.0.1:8080")
        )
      )
    }
  }

  it should "handle real json from real output" in {
    val json = """{"volumeId":"3","locations":[{"url":"127.0.0.1:8080","publicUrl":"127.0.0.1:8080"}]}"""
    val jsv = Json.parse( json )
    val jsr = jsv.validate[LookupResponse]
    assert(jsr.isSuccess, jsr)
    val expected = LookupResponse(
      volumeId = 3,
      locations = {
        val x127 = "127.0.0.1:8080"
        Seq(
          VolumeLocation(x127, x127)
        )
      }
    )
    jsr.get  shouldBe  expected
  }

}
