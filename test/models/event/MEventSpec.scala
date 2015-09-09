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

  "MEvent JSON" must {

    "handle fully-filled model" in {
      val mevent = MEvent(
        etype = MEventTypes.AdvExtTgError,
        ownerId = "asdasdadawdawd",
        argsInfo = ArgsInfo(
          adnIdOpt    = Some("asdasdasd1234"),
          advExtTgIds = Seq("asdasd", "asd3asgasgsadg"),
          adIdOpt     = Some("asdadsdasdasd42t24t"),
          advOkIdOpt  = Some(123),
          advReqIdOpt = Some(666),
          advRefuseIdOpt = Some(777)
        ),
        isCloseable = !MEvent.isCloseableDflt,
        isUnseen    = true,
        id          = Some("asdasdasdas7777d"),
        versionOpt  = Some(1)
      )

      MEvent.deserializeOne2(mevent)  mustBe  mevent
    }


    "handle minimally-filled model" in {
      val mevent = MEvent(
        etype = MEventTypes.AdvOutcomingRefused,
        ownerId = "asd"
      )

      MEvent.deserializeOne2(mevent)  mustBe  mevent
    }

  }

}
