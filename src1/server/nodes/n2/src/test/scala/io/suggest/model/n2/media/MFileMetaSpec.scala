package io.suggest.model.n2.media

import io.suggest.crypto.hash.MHashes
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 21:59
 * Description: Тесты для модели [[MFileMeta]].
 */
class MFileMetaSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MFileMeta

  private val minV = {
    MFileMeta(
      mime  = "application/json",
      sizeB = 132421,
      isOriginal = true,
      hashesHex = Nil,
    )
  }

  "JSON" should "handle minimal model" in {
    jsonTest(minV)
  }

  it should "handle full-filled model" in {
    jsonTest {
      minV.copy(
        hashesHex = Seq(
          MFileMetaHash(
            hType     = MHashes.Sha1,
            hexValue  = "asdioja4i8fa34fa43wf",
            flags     = Set.empty,
          )
        )
      )
    }
  }

}
