package io.suggest.model.n2.node

import org.scalatest._, Matchers._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 10:02
 * Description: Тесты для модели MNodeTypes
 */
class MNodeTypesSpec extends FlatSpec {

  private def t(mnt: MNodeType): Unit = {
    val jsv = Json.toJson(mnt)
    jsv.as[MNodeType] shouldBe mnt
  }

  "JSON" should "handle all values" in {
    for (v <- MNodeTypes.values) {
      t(v)
    }
  }

}
