package io.suggest.model.n2.media.storage

import io.suggest.model.PlayJsonTestUtil
import org.scalatest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 11:02
 * Description: Тесты для модели [[MStorages]].
 */
class MStoragesSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MStorage

  "JSON" should "support all values" in {
    MStorages.values
      .iterator
      .foreach { v =>
        jsonTest(v)
      }
  }

}
