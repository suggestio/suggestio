package io.suggest.model.n2.media.storage

import java.util.UUID

import io.suggest.model.PlayJsonTestUtil
import io.suggest.swfs.client.proto.fid.Fid
import org.scalatest.FlatSpec
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 11:23
 * Description: Тесты для абстрактной модели [[IMediaStorage]].
 */
class IMediaStorageSpec extends PlaySpec with OneAppPerSuite with PlayJsonTestUtil {

  override type T = IMediaStorage

  private lazy val swfsStorage = app.injector.instanceOf( classOf[SwfsStorage_] )

  "JSON" must {

    "support CassandraStorage" in {
      jsonTest {
        CassandraStorage(
          rowKey = UUID.randomUUID(),
          qOpt = None
        )
      }
    }

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
