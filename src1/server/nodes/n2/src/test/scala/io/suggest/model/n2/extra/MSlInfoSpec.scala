package io.suggest.model.n2.extra

import io.suggest.model.sc.common.AdShowLevels
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 22:55
 * Description: Тесты для модели [[MSlInfo]]
 */
class MSlInfoSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MSlInfo

  "JSON" should "support model" in {
    jsonTest(
      MSlInfo( AdShowLevels.LVL_START_PAGE, 100 )
    )
  }

}
