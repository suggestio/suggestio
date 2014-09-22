package io.suggest.util

import java.util.UUID
import UuidUtil._
import org.scalatest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.14 17:08
 * Description: Тесты для [[io.suggest.util.UuidUtil]].
 */
class UuidUtilTest extends FlatSpec with Matchers {

  UuidUtil.getClass.getSimpleName should "serialize and deserialize UUIDs to bytes" in {
    def ed: Unit = {
      val uuid = UUID.randomUUID()
      val uuidBytes = uuidToBytes(uuid)
      uuid  shouldEqual  bytesToUuid(uuidBytes)
    }
    times(ed)
  }

  it should "serialize/deserialize UUIDs from/to base64" in {
    def ed: Unit = {
      val uuid = UUID.randomUUID()
      val uuidB64 = uuidToBase64(uuid)
      base64ToUuid(uuidB64)  shouldEqual  uuid
    }
    times(ed)
  }


  private def times(f: => Any, t: Int = 100): Unit = {
    for(i <- 0 to t) {
      f
    }
  }

}
