package io.suggest.model.n2.ad.ent

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 14:38
 * Description: Тесты для модели [[Coords2d]].
 */
class Coords2dSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = Coords2d

  "play-json FORMAT" should "handle model" in {
    jsonTest {
      Coords2d(
        x = 100,
        y = -200
      )
    }
  }

}
