package models.event

import functional.OneAppPerSuiteNoGlobalStart
import org.scalatestplus.play.PlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 14:21
 * Description: Тесты для модели [[MEvent]].
 */
class MEventSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  private lazy val mEvents = app.injector.instanceOf[MEvents]

  "MEvent JSON" must {

    import mEvents.Implicits.mockPlayDocRespEv

    "handle fully-filled model" in {
      val mevent = MEvent(
        etype = MEventTypes.AdvExtTgError,
        ownerId = "asdasdadawdawd",
        argsInfo = ArgsInfo(
          adnIdOpt    = Some("asdasdasd1234"),
          advExtTgIds = Seq("asdasd", "asd3asgasgsadg"),
          adIdOpt     = Some("asdadsdasdasd42t24t")
        ),
        isCloseable = !MEvent.isCloseableDflt,
        isUnseen    = true,
        id          = Some("asdasdasdas7777d"),
        versionOpt  = Some(1)
      )

      mEvents.deserializeOne2(mevent)  mustBe  mevent
    }


    "handle minimally-filled model" in {
      val mevent = MEvent(
        etype = MEventTypes.AdvOutcomingRefused,
        ownerId = "asd"
      )

      mEvents.deserializeOne2(mevent)  mustBe  mevent
    }

  }

}
