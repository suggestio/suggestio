package io.suggest.common.uuid

import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 22:35
  * Description: Тесты для [[LowUuidUtil]].
  */
object LowUuidUtilSpec extends SimpleTestSuite {

  test("Must format hex string into uuid format") {
    val r = LowUuidUtil.hexStringToUuid( "b9407f30f5f8466eaff925556b57fe6d" )
    assertEquals(r, "b9407f30-f5f8-466e-aff9-25556b57fe6d")
  }

  test("Format hex string into EddyStone UID format") {
    val r = LowUuidUtil.hexStringToEddyUid("b9407f30f5f8466eaff925556b57fe6d" )
    assertEquals(r, "b9407f30f5f8466eaff9-25556b57fe6d")
  }

}
