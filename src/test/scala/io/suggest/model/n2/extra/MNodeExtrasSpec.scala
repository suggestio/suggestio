package io.suggest.model.n2.extra

import java.util.UUID

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 22:57
 * Description: Тесты для модели [[MNodeExtras]].
 */
class MNodeExtrasSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MNodeExtras

  "JSON" should "support minimal model" in {
    jsonTest( MNodeExtras.empty )
    jsonTest( MNodeExtras() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MNodeExtras(
        adn = Some(MAdnExtra(
          testNode  = true,
          isUser    = true
        )),
        beacon = Some(MBeaconExtra(
          uuidStr = UUID.randomUUID().toString,
          major   = 35666,
          minor   = 65354
        ))
      )
    }
  }

}
