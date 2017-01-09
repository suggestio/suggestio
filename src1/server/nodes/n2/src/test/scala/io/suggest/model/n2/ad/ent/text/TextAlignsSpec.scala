package io.suggest.model.n2.ad.ent.text

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 14:42
 * Description: Тесты для модели [[TextAligns]].
 */
class TextAlignsSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = TextAlign

  "play JSON" should "handle model elements" in {
    for (ta <- TextAligns.values) {
      jsonTest(ta)
    }
  }

}
