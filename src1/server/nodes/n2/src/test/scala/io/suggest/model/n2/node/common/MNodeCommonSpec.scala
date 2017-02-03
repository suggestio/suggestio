package io.suggest.model.n2.node.common

import io.suggest.model.n2.node.MNodeTypes
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 9:46
 * Description: Тесты для модели [[MNodeCommon]].
 */
class MNodeCommonSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MNodeCommon

  "JSON support" should "handle Ad" in {
    jsonTest(
      MNodeCommon(
        ntype = MNodeTypes.Ad,
        isDependent = true,
        isEnabled = true
      )
    )
  }

  it should "handle Image" in {
    jsonTest(
      MNodeCommon(
        ntype = MNodeTypes.Media.Image,
        isDependent = true
      )
    )
  }

  it should "handle disabled ADN node" in {
    jsonTest(
      MNodeCommon(
        ntype = MNodeTypes.AdnNode,
        isDependent = false,
        isEnabled = false,
        disableReason = Some("asd asd")
      )
    )
  }

}
