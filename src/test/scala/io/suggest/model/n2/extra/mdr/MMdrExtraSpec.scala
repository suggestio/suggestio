package io.suggest.model.n2.extra.mdr

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.10.15 17:03
 * Description: Тесты для подмодели [[MMdrExtra]].
 */
class MMdrExtraSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MMdrExtra

  "play.json" should "support empty model" in {
    jsonTest( MMdrExtra() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MMdrExtra(
        freeAdv = Some(MFreeAdv(
          isAllowed = true,
          byUser    = "fa4908f34k90f34__43t34f"
        ))
      )
    }
  }

}
