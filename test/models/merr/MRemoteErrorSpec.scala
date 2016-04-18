package models.merr

import functional.OneAppPerSuiteNoGlobalStart
import io.suggest.model.geo.GeoPoint
import org.scalatestplus.play.PlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 14:30
 * Description: Тесты для модели [[MRemoteError]].
 */
class MRemoteErrorSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  private lazy val mRemoteErrors = app.injector.instanceOf[MRemoteErrors]

  "MRemoteError JSON" must {

    import mRemoteErrors.mockPlayDocRespEv

    "handle partially-filled model" in {
      val merr = MRemoteError(
        errorType   = MRemoteErrorTypes.Showcase,
        msg         = "hello world!",
        clientAddr  = "127.0.0.1"
      )

      mRemoteErrors.deserializeOne2(merr)  mustBe  merr
    }


    "handle fully-filled model" in {
      val merr = MRemoteError(
        errorType   = MRemoteErrorTypes.Showcase,
        msg         = "hello world!",
        clientAddr  = "127.0.0.1",
        ua          = Some("Mozilla/4.0 compatible"),
        url         = Some("HTTP://suggest.io/asdasd/123?zsdrfvb"),
        clIpGeo     = Some( GeoPoint(10, 20) ),
        clTown      = Some("Чугуев"),
        country     = Some("Эрафия"),
        isLocalCl   = Some(true),
        state       = Some("""{"a":1, "asdasd":{ }}"""),
        id          = Some("ASDAWDAWD3252535")
      )

      mRemoteErrors.deserializeOne2(merr)  mustBe  merr
    }

  }

}
