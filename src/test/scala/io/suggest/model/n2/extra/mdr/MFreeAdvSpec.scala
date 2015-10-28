package io.suggest.model.n2.extra.mdr

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.10.15 16:25
 * Description: Тесты для под-модели [[MFreeAdv]].
 */
class MFreeAdvSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MFreeAdv

  "play.json FORMAT" should "support minimal model" in {
    jsonTest {
      MFreeAdv(
        isAllowed = true,
        byUser    = "fa4tr3a4908fkj3940f43r"
      )
    }
  }

  it should "support full-filled model" in {
    jsonTest {
      MFreeAdv(
        isAllowed = false,
        byUser = "F354490gk35gks4r90g4_",
        reason = Some("nomad.jpg")
      )
    }
  }

}
