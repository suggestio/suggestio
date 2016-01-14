package io.suggest.model.n2.node.meta.colors

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 16:53
 * Description: Тесты для модели-карты цветов [[MColors]].
 */
class MColorsSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MColors

  "JSON" should "handle empty model" in {
    jsonTest(MColors.empty)
    jsonTest(MColors())
  }

  it should "handle fullfilled model" in {
    jsonTest {
      MColors(
        bg = Some( MColorData("AAAAAA") ),
        fg = Some( MColorData("FFFFFF") ),
        pattern = Some( MColorData("123123") )
      )
    }
  }

}
