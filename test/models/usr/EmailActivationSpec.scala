package models.usr

import functional.OneAppPerSuiteNoGlobalStart
import org.scalatestplus.play.PlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 15:00
 * Description: Тесты для модели [[EmailActivation]].
 */
class EmailActivationSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  "EmailActivation JSON" must {

    "handle fully-filled model" in {
      val mea = EmailActivation(
        email = "asdasdasd@thrthrth.ra",
        key   = EmailActivation.randomActivationKey,
        id    = Some("asdasd89asd89asdijas")
      )

      EmailActivation.deserializeOne2(mea)  mustBe  mea
    }

  }

}
