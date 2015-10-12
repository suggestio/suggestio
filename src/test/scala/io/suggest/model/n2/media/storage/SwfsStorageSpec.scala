package io.suggest.model.n2.media.storage

import io.suggest.model.PlayJsonTestUtil
import io.suggest.model.n2.media.storage.swfs.{SwfsStorage_, SwfsStorage}
import io.suggest.swfs.client.proto.fid.Fid
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 11:08
 * Description: Тесты для под-модели [[SwfsStorage]].
 */
class SwfsStorageSpec extends PlaySpec with OneAppPerSuite with PlayJsonTestUtil {

  override type T = SwfsStorage

  private lazy val swfsStorage = app.injector.instanceOf( classOf[SwfsStorage_] )

  "JSON" must {

    "support model" in {
      jsonTest {
        swfsStorage(
          Fid(
            volumeId  = 1,
            fileId    = "42abcdef123"
          )
        )
      }
    }

  }

}
