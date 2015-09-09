package models.usr

import functional.OneAppPerSuiteNoGlobalStart
import models.mext.MExtServices
import org.scalatestplus.play.PlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 15:07
 * Description: Тесты для модели [[MExtIdent]].
 */
class MExtIdentSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  "MExtIdent JSON" must {

    "handle fully-filled model" in {
      val meid = MExtIdent(
        personId = "asda9wdajk9w0dAWdawd8",
        provider = MExtServices.FACEBOOK.loginProvider.get,
        userId   = "1314284918948912489",
        email    = Some("asdawd.ergerg.g@fgdasfd.grt"),
        versionOpt = Some(1)
      )

      MExtIdent.deserializeOne2(meid)  mustBe  meid
    }

  }

}
