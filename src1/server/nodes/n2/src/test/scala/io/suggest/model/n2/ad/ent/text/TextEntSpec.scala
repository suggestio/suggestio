package io.suggest.model.n2.ad.ent.text

import io.suggest.common.geom.coord.MCoords2di
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 17:02
 * Description: Тесты для модели [[TextEnt]].
 */
class TextEntSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = TextEnt

  "play JSON" should "handle model" in {
    jsonTest {
      TextEnt(
        value = "test test hellow workd",
        font  = EntFont(
          color = "afafaf"
        ),
        coords = Some(MCoords2di(100, 200))
      )
    }
  }

}
