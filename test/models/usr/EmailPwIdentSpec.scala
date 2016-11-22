package models.usr

import functional.OneAppPerSuiteNoGlobalStart
import org.scalatestplus.play.PlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 15:03
 * Description: Тесты для модели [[EmailPwIdent]].
 */
class EmailPwIdentSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  private lazy val emailPwIdents = app.injector.instanceOf[EmailPwIdents]
  private lazy val emailPwIdentsImplicits = emailPwIdents.Implicits

  "EmailPwIdent JSON" must {

    import emailPwIdentsImplicits.mockPlayDocRespEv

    "handle fully-filled model" in {
      val epw = EmailPwIdent(
        email       = "asdasd893.ghserger@53tsdfsd.opr",
        personId    = "Aasdfa9sr3dka390dka3",
        pwHash      = "ad8q39djq38djq23dq23q2g89gu5gj34g",
        isVerified  = true
      )

      emailPwIdents.deserializeOne2(epw)  mustBe  epw
    }

  }

}
