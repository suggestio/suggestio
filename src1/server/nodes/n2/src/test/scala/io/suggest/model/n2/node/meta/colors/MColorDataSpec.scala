package io.suggest.model.n2.node.meta.colors

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 16:50
 * Description: Тесты для модели [[MColorData]].
 */
class MColorDataSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MColorData

  "JSON" should "handle minimal model" in {
    jsonTest( MColorData("FFFFFF") )
  }

}
