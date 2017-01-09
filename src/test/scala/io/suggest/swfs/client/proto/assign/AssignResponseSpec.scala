package io.suggest.swfs.client.proto.assign

import io.suggest.PlayJsonTesting
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import play.api.libs.json.{JsSuccess, Json}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 10:02
 * Description: Тесты для модели [[AssignResponse]].
 */
class AssignResponseSpec extends FlatSpec with PlayJsonTesting {

  "JSON Format" should "support model" in {
    _jsonTest {
      AssignResponse(
        count = 1,
        fid   = "3,01637037d6",
        url   = "127.0.0.1:8080",
        publicUrl = "localhost:8080"
      )
    }
  }

  // https://github.com/chrislusf/seaweedfs/blob/master/README.md
  it should "deserialize documented examples" in {
    val json = """{"count":1,"fid":"3,01637037d6","url":"127.0.0.1:8080","publicUrl":"localhost:8080"}"""
    val parsed = AssignResponse(
      count = 1,
      fid   = "3,01637037d6",
      url   = "127.0.0.1:8080",
      publicUrl = "localhost:8080"
    )

    val jsv = Json.parse( json )
    jsv.validate[AssignResponse]  shouldBe  JsSuccess(parsed)
  }

}
