package util.stat

import java.util.UUID

import org.scalatestplus.play._
import play.api.GlobalSettings
import play.api.test.FakeApplication

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.14 12:04
 * Description:
 */
class StatUtilSpec extends PlaySpec with OneAppPerSuite {

  import StatUtil._


  /** Штатный Global производит долгую инициализацию, которая нам не нужен.
    * Нужен только доступ к конфигу. Ускоряем запуск: */
  override implicit lazy val app = FakeApplication(
    withGlobal = Some(new GlobalSettings() {
      override def onStart(app: play.api.Application) {
        super.onStart(app)
        println("Started dummy fake application, without Global.onStart() initialization.")
      }
    })
  )


  // Тесты

  "StatUtil" must {

    "serialize and deserialize UUIDs to bytes" in {
      def ed: Unit = {
        val uuid = UUID.randomUUID()
        val uuidBytes = uuidToBytes(uuid)
        uuid  mustBe  bytesToUuid(uuidBytes)
      }
      times(ed)
    }


    "serialize and deserialize UUIDs to String+MAC" in {
      def ed: Unit = {
        val uuid = UUID.randomUUID()
        val cookieVal = mkUidCookieValue(uuid)
        deserializeCookieValue(cookieVal)  mustBe  Some(uuid)
      }
      times(ed)
    }

    "serialize/deserialize UUIDs from/to base64" in {
      def ed: Unit = {
        val uuid = UUID.randomUUID()
        val uuidB64 = uuidToBase64(uuid)
        base64ToUuid(uuidB64)  mustBe  uuid
      }
      times(ed)
    }

  }


  private def times(f: => Any, t: Int = 100): Unit = {
    for(i <- 0 to t) {
      f
    }
  }

}
