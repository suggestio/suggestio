package models.usr

import functional.OneAppPerSuiteNoGlobalStart
import org.scalatestplus.play.PlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 15:12
 * Description: Тесты для модели [[MPerson]].
 */
class MPersonSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  "MPerson JSON" must {

    "handle model instance" in {
      val mperson = MPerson(
        lang = "ru",
        id   = Some("aSdaw89djkawidoawd")
      )

      MPerson.deserializeOne2(mperson)  mustBe  mperson
    }

  }

}
