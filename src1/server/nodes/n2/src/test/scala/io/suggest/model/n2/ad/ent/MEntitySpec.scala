package io.suggest.model.n2.ad.ent

import io.suggest.common.geom.coord.MCoords2di
import io.suggest.model.n2.ad.ent.text.{EntFont, TextEnt}
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 17:48
 * Description: Тесты для json-модели [[MEntity]].
 */
class MEntitySpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MEntity

  "play JSON" should "support text entity" in {
    jsonTest {
      MEntity(
        id = 1,
        text = Some(
          TextEnt(
            value = "gasegr esrg serg",
            font  = EntFont()
          )
        ),
        coords = Some(MCoords2di(100, 200))
      )
    }
  }

}
