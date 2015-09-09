package models.adv

import functional.OneAppPerSuiteNoGlobalStart
import models.mext.MExtServices
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 11:08
 * Description: Тесты для хранимой модели [[MExtTarget]]. Интересует сериализация-десериализация.
 */
class MExtTargetSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  "MExtTarget JSON" must {

    "handle fully-filled model" in {
      val mes = MExtTarget(
        url         = "https://vk.com/null",
        service     = MExtServices.VKONTAKTE,
        adnId       = "235434553aASDGg",
        id          = Some("asdasdasdasdasd"),
        versionOpt  = Some(123L)
      )

      MExtTarget.deserializeOne2(mes) mustBe mes
    }

    "handle minimally-filled model" in {
      val mes = MExtTarget(
        url     = "https://facebook.com/me",
        service = MExtServices.FACEBOOK,
        adnId   = "asdasdjauiwdjaw1234234"
      )

      MExtTarget.deserializeOne2(mes)  mustBe  mes
    }

  }

}
