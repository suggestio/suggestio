package models.sec

import functional.OneAppPerSuiteNoGlobalStart
import org.scalatestplus.play.PlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 14:42
 * Description: Тесты для модели [[MAsymKey]].
 */
class MAsymKeySpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  private val KEY = "ASDASDASDawef0awefawfu89a3ufa3jf3ajfajdfasrfAWEF awefawefawef a90wer902348r23r/sd/fsdf3"

  "MAsymKey JSON" must {

    "handle minimally-filled model" in {
      val masc = MAsymKey(
        pubKey = KEY,
        secKey = None,
        id     = None
      )

      MAsymKey.deserializeOne2(masc)  mustBe  masc
    }

    "handle fully-filled model" in {
      val masc = MAsymKey(
        pubKey      = KEY,
        secKey      = Some(KEY.reverse),
        id          = Some("ASDasdasd89asd89asd"),
        versionOpt  = Some(2)
      )

      MAsymKey.deserializeOne2(masc)  mustBe  masc
    }

  }

}
