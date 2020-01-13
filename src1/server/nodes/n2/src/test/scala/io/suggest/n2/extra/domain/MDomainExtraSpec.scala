package io.suggest.n2.extra.domain

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.16 18:54
  * Description: Тесты для модели [[MDomainExtra]].
  */
class MDomainExtraSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MDomainExtra

  "MDomainExtra" should "support normally filled model" in {
    jsonTest {
      MDomainExtra(
        dkey = "suggest.io",
        mode = MDomainModes.ScServeIncomingRequests
      )
    }
  }

}
