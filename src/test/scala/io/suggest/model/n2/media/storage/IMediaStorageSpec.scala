package io.suggest.model.n2.media.storage

import java.util.UUID

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 11:23
 * Description: Тесты для абстрактной модели [[IMediaStorage]].
 */
class IMediaStorageSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = IMediaStorage

  "JSON" should "support CassandraStorage" in {
    jsonTest {
      CassandraStorage(
        rowKey = UUID.randomUUID(),
        qOpt = None
      )
    }
  }

  it should "support SwfsStorage" in {
    jsonTest {
      SwfsStorage(
        volumeId = 4L,
        fileId = "asdasdad920i435rkt3io54f"
      )
    }
  }

}
