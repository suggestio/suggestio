package io.suggest.model.n2.media

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 22:09
 * Description: Тесты для модели [[MPictureMeta]].
 */
class MPictureMetaSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MPictureMeta

  "JSON" should "handle model" in {
    jsonTest(
      MPictureMeta(
        width   = 1200,
        height  = 800
      )
    )
  }

}
