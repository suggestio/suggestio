package io.suggest.n2.ad.ent

import io.suggest.common.geom.coord.MCoords2di
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 14:38
 * Description: Тесты для модели [[MCoords2di]].
 */
class Coords2dSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MCoords2di

  "play-json FORMAT" should "handle model" in {
    jsonTest {
      MCoords2di(
        x = 100,
        y = -200
      )
    }
  }

}
