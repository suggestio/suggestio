package io.suggest.model.n2.node.meta

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 13:15
 * Description: Тесты для модели метаданных [[MAddress]].
 */
class MAddressSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MAddress

  "JSON" should "handle minimal/empty model" in {
    jsonTest( MAddress.empty )
    jsonTest( MAddress() )
  }

  it should "handle full-filled model" in {
    jsonTest {
      MAddress(
        town      = Some("Санкт-Петрозаводск"),
        address   = Some("пл.Ленина, дом 1,\n корпус 2, строение 3Б, \nквартирка номер четыре."),
        phone     = Some("+8-999-354-7642"),
        floor     = Some("-1"),
        section   = Some("333а")
      )
    }
  }

}
