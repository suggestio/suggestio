package io.suggest.model.n2.extra

import io.suggest.adn.MAdnRights
import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 22:51
 * Description: Тесты для модели [[MAdnExtra]], которая является подмоделью [[MNodeExtras]].
 */
class MAdnExtraSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MAdnExtra


  "JSON" should "support minimal model" in {
    jsonTest( MAdnExtra() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MAdnExtra(
        rights = Set( MAdnRights.PRODUCER, MAdnRights.RECEIVER ),
        isUser = true,
        shownTypeIdOpt = Some("a"),
        testNode = true,
        showInScNl = true
      )
    }
  }

}
