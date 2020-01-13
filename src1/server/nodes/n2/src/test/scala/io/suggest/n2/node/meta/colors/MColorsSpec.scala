package io.suggest.n2.node.meta.colors

import io.suggest.color.{MColorData, MColors}
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 16:53
 * Description: Тесты для модели-карты цветов [[MColors]].
 */
class MColorsSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MColors

  "JSON" should "handle empty model" in {
    jsonTest(MColors.empty)
    jsonTest(MColors())
  }

  it should "handle fullfilled model" in {
    jsonTest {
      MColors(
        bg = Some( MColorData("AAAAAA") ),
        fg = Some( MColorData("FFFFFF") )
      )
    }
  }

}
