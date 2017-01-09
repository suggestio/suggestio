package io.suggest.swfs.client.proto.lookup

import io.suggest.PlayJsonTesting
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 14:46
 * Description: Тесты для JSON-модели [[VolumeLocation]].
 */
class VolumeLocationSpec extends FlatSpec with PlayJsonTesting {

  private val m0 = {
    VolumeLocation(
      publicUrl = "localhost:8080",
      url       = "127.0.0.1:8080"
    )
  }

  "JSON" should "handle model" in {
    _jsonTest(m0)
  }

  it should "handle raw json from real output" in {
    val json = """ {"url":"127.0.0.1:8080","publicUrl":"localhost:8080"} """
    val jsv = Json.parse(json)
    val jsr = VolumeLocation.FORMAT.reads(jsv)
    assert(jsr.isSuccess, jsr)
    jsr.get  shouldBe  m0
  }

}
