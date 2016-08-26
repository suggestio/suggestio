package io.suggest.model.n2.extra

import java.util.UUID

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.08.16 18:00
  * Description: Тесты для модели [[MBeaconExtra]].
  */
class MBeaconExtraSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MBeaconExtra

  "MBeaconExtra" should "support minimally filled model" in {
    jsonTest {
      MBeaconExtra(
        uuidStr = UUID.randomUUID().toString,
        major   = 2342,
        minor   = 56346
      )
    }
  }

}
