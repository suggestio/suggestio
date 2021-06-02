package io.suggest.n2.extra

import io.suggest.n2.extra.domain.{MDomainExtra, MDomainModes}
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 22:57
 * Description: Тесты для модели [[MNodeExtras]].
 */
class MNodeExtrasSpec extends AnyFlatSpec with PlayJsonTestUtil {

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
        domains = Seq(
          MDomainExtra(
            dkey = "suggest.io",
            mode = MDomainModes.ScServeIncomingRequests
          ),
          MDomainExtra(
            dkey = "xata.ok",
            mode = MDomainModes.ScServeIncomingRequests
          )
        )
      )
    }
  }

}
