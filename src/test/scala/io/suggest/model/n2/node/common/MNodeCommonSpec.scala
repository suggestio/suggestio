package io.suggest.model.n2.node.common

import io.suggest.model.n2.node.MNodeTypes
import org.scalatest._, Matchers._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 9:46
 * Description: Тесты для модели [[MNodeCommon]].
 */
class MNodeCommonSpec extends FlatSpec {

  private def t(mn: MNodeCommon): Unit = {
    val jsv = Json.toJson(mn)
    val parsed = jsv.as[MNodeCommon]
    parsed  shouldBe  mn
  }

  "JSON support" should "handle Ad" in {
    t(MNodeCommon(
      ntype = MNodeTypes.Ad,
      isDependent = true
    ))
  }

  it should "handle Image" in {
    t(MNodeCommon(
      ntype = MNodeTypes.Media.Image,
      isDependent = true
    ))
  }

  it should "handle ADN node" in {
    t(MNodeCommon(
      ntype = MNodeTypes.AdnNode,
      isDependent = false
    ))
  }

}
