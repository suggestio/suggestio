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

  private lazy val emailActivations = app.injector.instanceOf[EmailActivations]
  private lazy val emailActivationsImplicits = emailActivations.Implicits

  "EmailActivation JSON" must {

    import emailActivationsImplicits.mockPlayDocRespEv

    "handle fully-filled model" in {
      val mea = EmailActivation(
        email = "asdasdasd@thrthrth.ra",
        key   = EmailActivation.randomActivationKey,
        id    = Some("asdasd89asd89asdijas")
      )

      emailActivations.deserializeOne2(mea)  mustBe  mea
    }

  }

}
