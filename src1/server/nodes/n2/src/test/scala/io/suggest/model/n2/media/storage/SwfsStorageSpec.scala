package io.suggest.model.n2.media.storage

import io.suggest.model.n2.media.storage.swfs.SwfsStorage
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatestplus.play.PlaySpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 11:08
 * Description: Тесты для под-модели [[SwfsStorage]].
 */
class SwfsStorageSpec extends PlaySpec with PlayJsonTestUtil {

  override type T = SwfsStorage

  "JSON" must {

    "support model" in {
      jsonTest {
        SwfsStorage(
          Fid(
            volumeId  = 1,
            fileId    = "42abcdef123"
          )
        )
      }
    }

  }

}
