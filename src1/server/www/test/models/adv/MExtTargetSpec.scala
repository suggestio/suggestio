package models.adv

import functional.OneAppPerSuiteNoGlobalStart
import io.suggest.ext.svc.MExtServices
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
    import mExtTargets.Implicits.mockPlayDocRespEv

    "handle fully-filled model" in {
      val mes = MExtTarget(
        url         = "https://vk.com/null",
        service     = MExtServices.VKontakte,
        adnId       = "235434553aASDGg",
        id          = Some("asdasdasdasdasd"),
        versioning  = Some(123L)
      )

      mExtTargets.deserializeOne2(mes) mustBe mes
    }

    "handle minimally-filled model" in {
      val mes = MExtTarget(
        url     = "https://facebook.com/me",
        service = MExtServices.FaceBook,
        adnId   = "asdasdjauiwdjaw1234234"
      )

      mExtTargets.deserializeOne2(mes)  mustBe  mes
    }

  }

}
