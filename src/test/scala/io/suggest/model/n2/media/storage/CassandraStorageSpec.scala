package io.suggest.model.n2.media.storage

import java.util.UUID

import io.suggest.model.PlayJsonTestUtil
import org.scalatest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 11:04
 * Description: Тесты для под-модели [[CassandraStorage]].
 */
class CassandraStorageSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = CassandraStorage

  "JSON" should "handle minimal model" in {
    jsonTest {
      CassandraStorage(
        rowKey  = UUID.randomUUID(),
        qOpt    = None
      )
    }
  }

  it should "handle full-filled model" in {
    jsonTest {
      CassandraStorage(
        rowKey = UUID.randomUUID(),
        qOpt   = Some("x=h&a=1&c=1231x654j&")
      )
    }
  }

}
