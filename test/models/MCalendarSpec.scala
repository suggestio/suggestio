package models

import functional.OneAppPerSuiteNoGlobalStart
import org.scalatestplus.play.PlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 14:17
 * Description: Тесты для модели календаря.
 */
class MCalendarSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  lazy val mCalendar = app.injector.instanceOf[MCalendars]

  "MCalendar JSON" must {

    "handle fully-filled fields" in {
      val mcal = MCalendar(
        name        = "RUSSIAN prime",
        data        = """asdasd\n\asddasd\53535\t\n\sdfasdf """,
        id          = Some("aADw4312rqefasef"),
        versionOpt  = Some(1),
        companion   = mCalendar
      )

      mCalendar.deserializeOne2(mcal) mustBe mcal
    }


    "handle partially-filled model" in {
      val mcal = MCalendar(
        name        = "RUSSIAN prime календарррррь",
        data        = """ asdasd\n\asddasd\53535\t\n\sdf$$##asdf\n#\n\n\n\t\rasdasd  """,
        id          = None,
        versionOpt  = None,
        companion   = mCalendar
      )

      mCalendar.deserializeOne2(mcal) mustBe mcal
    }

  }

}
