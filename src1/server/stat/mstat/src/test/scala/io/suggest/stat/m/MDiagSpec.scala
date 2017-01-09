package io.suggest.stat.m

import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 14:22
  * Description: Тесты для модели [[MDiag]].
  */
class MDiagSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MDiag

  "JSON" should "support empty model" in {
    jsonTest( MDiag() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MDiag(
        message = Some("всё сломалось, ничего не работает, что делать111"),
        state   = Some("ScJsState(1,2,3,...)")
      )
    }
  }

}
