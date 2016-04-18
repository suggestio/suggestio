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

  private lazy val mExtTargets = app.injector.instanceOf[MExtTargets]

  "MExtTarget JSON" must {

    import mExtTargets.mockPlayDocRespEv

    "handle fully-filled model" in {
      val mes = MExtTarget(
        url         = "https://vk.com/null",
        service     = MExtServices.VKONTAKTE,
        adnId       = "235434553aASDGg",
        id          = Some("asdasdasdasdasd"),
        versionOpt  = Some(123L)
      )

      mExtTargets.deserializeOne2(mes) mustBe mes
    }

    "handle minimally-filled model" in {
      val mes = MExtTarget(
        url     = "https://facebook.com/me",
        service = MExtServices.FACEBOOK,
        adnId   = "asdasdjauiwdjaw1234234"
      )

      mExtTargets.deserializeOne2(mes)  mustBe  mes
    }

  }

}
