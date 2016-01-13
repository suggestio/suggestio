package io.suggest.model.n2.media.storage

import io.suggest.model.PlayJsonTestUtil
import io.suggest.swfs.client.proto.fid.Fid
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 11:23
 * Description: Тесты для абстрактной модели [[IMediaStorage]].
 */
class IMediaStorageSpec extends PlaySpec with OneAppPerSuite with PlayJsonTestUtil {

  override type T = IMediaStorage

  private lazy val iMediaStorages = app.injector.instanceOf[IMediaStorages]

  private def swfsStorage = iMediaStorages.swfsStorage

  "JSON" must {

    import iMediaStorages.FORMAT

    "support SwfsStorage" in {
      jsonTest {
        swfsStorage(
          Fid(
            volumeId = 4,
            fileId = "asdasdad920i435rkt3io54f"
          )
        )
      }
    }

  }

}
