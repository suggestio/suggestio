package io.suggest.ctx

import java.util.UUID

import io.suggest.util.UuidUtil
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import play.api.Configuration
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 12:28
  * Description: Тесты для [[MCtxId]] и [[MCtxIds]].
  */
class MCtxIdSpec extends AnyFlatSpec {

  private val mCtxIds = {
    val configuration = Configuration(
      MCtxId.SECKET_KEY_CONF_NAME -> "asdasdasdasdasdasdasdasdasdasd"
    )
    new MCtxIds( configuration )
  }

  private def _personIdOpt(i: Int): Option[String] = {
    // Пусть каждый шестой результат будет None, что по смыслу есть personId анонимуса.
    if (i % 6 ==* 0) {
      None
    } else {
      Some( UuidUtil.uuidToBase64( UUID.randomUUID() ) )
    }
  }

  classOf[MCtxIds].getSimpleName must "generate and validate signatures" in {
    for (i <- 1 to 100) {
      val m = mCtxIds( _personIdOpt(i) )
      mCtxIds.checkSig(m) shouldBe true
    }
  }

  it must "successfully validate sigs after serialization" in {
    for (i <- 1 to 100) {
      val m0 = mCtxIds( _personIdOpt(i) )
      val m2Opt = MCtxId.fromString( MCtxId.intoString(m0) )
      m2Opt.isDefined shouldBe true

      val m2 = m2Opt.get
      m2 shouldEqual m0

      mCtxIds.checkSig(m2) shouldBe true
    }
  }

}
