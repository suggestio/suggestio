package util.stat

import java.util.UUID

import functional.OneAppPerSuiteNoGlobalStart
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.14 12:04
 * Description: Тесты для [[util.stat.StatCookiesUtil]].
 */
class StatCookiesUtilSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  lazy val statUtil = app.injector.instanceOf[StatCookiesUtil]


  "StatUtil" must {

    import statUtil._

    "serialize and deserialize UUIDs to String+MAC" in {
      def ed: Unit = {
        val uuid = UUID.randomUUID()
        val cookieVal = mkUidCookieValue(uuid)
        deserializeCookieValue(cookieVal)  mustBe  Some(uuid)
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
