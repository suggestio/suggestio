package io.suggest.model.n2.media

import io.suggest.model.PlayJsonTestUtil
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
      sha1  = None
    )
  }

  "JSON" should "handle minimal model" in {
    jsonTest(minV)
  }

  it should "handle full-filled model" in {
    jsonTest {
      minV.copy(
        sha1 = Some("asdioja4i8fa34fa43wf")
      )
    }
  }

}
