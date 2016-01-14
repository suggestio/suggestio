package io.suggest.model.n2.extra

import io.suggest.model.PlayJsonTestUtil
import io.suggest.model.n2.extra.tag.{MTagExtra, MTagFace}
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 22:57
 * Description: Тесты для модели [[MNodeExtras]].
 */
class MNodeExtrasSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MNodeExtras

  "JSON" should "support minimal model" in {
    jsonTest( MNodeExtras.empty )
    jsonTest( MNodeExtras() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MNodeExtras(
        tag = Some(MTagExtra(
          faces = MTagFace.faces2map(Seq(
            MTagFace("ягоды"), MTagFace("грибы")
          ))
        )),
        adn = Some(MAdnExtra(
          testNode  = true,
          isUser    = true
        ))
      )
    }
  }

}
