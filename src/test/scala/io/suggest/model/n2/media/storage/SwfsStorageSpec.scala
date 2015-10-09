package io.suggest.model.n2.media.storage

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 11:08
 * Description: Тесты для под-модели [[SwfsStorage]].
 */
class SwfsStorageSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = SwfsStorage

  "JSON" should "support model" in {
    jsonTest {
      SwfsStorage(
        volumeId  = 1L,
        fileId    = "42abcdef123"
      )
    }
  }

}
