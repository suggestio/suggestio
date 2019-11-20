package io.suggest.model.n2.media.storage

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 11:02
 * Description: Тесты для модели [[MStorages]].
 */
class MStoragesSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MStorage

  "JSON" should "support all values" in {
    MStorages.values
      .iterator
      .foreach { v =>
        jsonTest(v)
      }
  }

}
