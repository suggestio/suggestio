package io.suggest.model.n2.tag

import io.suggest.model.n2.tag.vertex.{MTagFace, MTagVertex}
import org.scalatest._, Matchers._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.15 14:51
 * Description: Модель-контейнер для tag-подмоделей.
 */
class MNodeTagInfoSpec extends FlatSpec {

  private def t(mnti: MNodeTagInfo): Unit = {
    val jsVal = Json.toJson(mnti)
    val mapped = jsVal.validate[MNodeTagInfo]
    assert(mapped.isSuccess, mapped)
    mapped.get  shouldBe  mnti
  }

  "MNodeTagInfo" should "support EMPTY JSON serialize/deserialize" in {
    t( MNodeTagInfo.empty )
  }

  it should "support NON-EMPTY JSON" in {
    t {
      MNodeTagInfo(
        vertex = Some(MTagVertex(
          faces = MTagFace.faces2map(Seq(
            MTagFace("ягоды"), MTagFace("грибы")
          ))
        ))
      )
    }
  }

}
