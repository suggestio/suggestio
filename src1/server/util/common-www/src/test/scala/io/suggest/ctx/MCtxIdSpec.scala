package io.suggest.ctx

import org.scalatest._
import org.scalatest.Matchers._
import play.api.Configuration

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 12:28
  * Description: Тесты для [[MCtxId]] и [[MCtxIds]].
  */
class MCtxIdSpec extends FlatSpec {

  private val mCtxIds = {
    val configuration = Configuration(
      MCtxId.SECKET_KEY_CONF_NAME -> "asdasdasdasdasdasdasdasdasdasd"
    )
    new MCtxIds( configuration )
  }

  classOf[MCtxIds].getSimpleName must "generate and validate signatures" in {
    for (_ <- 1 to 100) {
      val m = mCtxIds()
      mCtxIds.verify(m) shouldBe true
    }
  }

  it must "successfully validate sigs after serialization" in {
    for (_ <- 1 to 100){
      val m0 = mCtxIds()
      val m2Opt = MCtxId.fromString( MCtxId.intoString(m0) )
      m2Opt.isDefined shouldBe true

      val m2 = m2Opt.get
      m2 shouldEqual m0

      mCtxIds.verify(m2) shouldBe true
    }
  }

}
