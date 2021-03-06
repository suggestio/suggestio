package io.suggest.n2.node.meta

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 13:15
 * Description: Тесты для модели метаданных [[MAddress]].
 */
class MAddressSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MAddress

  "JSON" should "handle minimal/empty model" in {
    jsonTest( MAddress.empty )
    jsonTest( MAddress() )
  }

  it should "handle full-filled model" in {
    jsonTest {
      MAddress(
        town      = Some("Санкт-Петрозаводск"),
        address   = Some("пл.Ленина, дом 1,\n корпус 2, строение 3Б, \nквартирка номер четыре.")
      )
    }
  }

}
