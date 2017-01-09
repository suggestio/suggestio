package io.suggest.model.n2.node.meta

import org.scalatest._, Matchers._
import play.api.libs.json.Json
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 10:50
 * Description: Тесты для модели [[MPersonMeta]].
 */
class MPersonMetaSpec extends FlatSpec {

  private def t(mpm: MPersonMeta): Unit = {
    val jsv = Json.toJson(mpm)
    jsv.as[MPersonMeta] shouldBe mpm
  }

  "JSON" should "handle empty model" in {
    t(MPersonMeta.empty)
    t(MPersonMeta())
  }
  
  it should "handle fullfilled model" in {
    t(MPersonMeta(
      nameFirst   = Some("Иван"),
      nameLast    = Some("Petrovf"),
      extAvaUrls  = List("https://img.vpashe.ru/img1.jpg")
    ))
  }

}
