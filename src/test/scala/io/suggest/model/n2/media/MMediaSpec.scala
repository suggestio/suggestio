package io.suggest.model.n2.media

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 22:51
 * Description: Тесты для модели [[MMedia]].
 */
class MMediaSpec extends FlatSpec {

  private def t(mn: MMedia): Unit = {
    MMedia.deserializeOne2(mn)  shouldBe  mn
  }

  private val m1 = {
    MMedia(
      nodeId = "asdarfg9345tkf34gf3g",
      file = MFileMeta(
        mime  = "application/jpeg",
        sizeB = 234525,
        sha1  = None
      ),
      id = None
    )
  }

  "JSON" should "handle minimal model" in {
    t(m1)
  }

  it should "handle full-filled model" in {
    t {
      m1.copy(
        picture = Some(MPictureMeta(
          width   = 640,
          height = 480
        )),
        id = Some( "asdaffafr23?awf349025234=f3w4fewfgse98ug3jg" ),
        versionOpt = Some(45L)
      )
    }
  }

}
