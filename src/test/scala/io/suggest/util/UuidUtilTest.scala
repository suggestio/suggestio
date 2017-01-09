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

  private def cn = UuidUtil.getClass.getSimpleName

  cn should "serialize and deserialize UUIDs to bytes" in {
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

  it should "deserialize garbaged id" in {
    base64ToUuid("onhyBuwLQX-o0wb54EnAHA/c=0x1200d&a=1600x1200_431_0&i=a&h=&d=a&j=b&f=70") shouldEqual base64ToUuid("onhyBuwLQX-o0wb54EnAHA")
  }

  // TODO Не отрабатывается реальный сериализованный uuid, взятый со страниц проекта, пишет что длины не хватает (EOF Exception)
  /*it should "deserialize AUwmoZDPxREC6sYH-YrU" in {
    assert( isUuidStrValid("AUwmoZDPxREC6sYH-YrU") )
  }*/

  private def times(f: => Any, t: Int = 100): Unit = {
    for(i <- 0 to t) {
      f
    }
  }

}
