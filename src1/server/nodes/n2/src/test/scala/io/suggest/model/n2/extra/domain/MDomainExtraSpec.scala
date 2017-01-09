package io.suggest.model.n2.extra.domain

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.16 18:54
  * Description: Тесты для модели [[MDomainExtra]].
  */
class MDomainExtraSpec extends FlatSpec with PlayJsonTestUtil {

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
