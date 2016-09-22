package io.suggest.stat.m

import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 10:52
  * Description: Тесты для модели [[MUa]].
  */
class MUaSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MUa

  "JSON" should "support empty model" in {
    jsonTest( MUa() )
  }

  it should "support model with all fields defined" in {
    jsonTest {
      MUa(
        ua        = Some( "Mozilla 5.0/compatible" ),
        browser   = Some( "firefox" ),
        device    = Some( "desktop" ),
        osFamily  = Some( "pravoslavnaya OS" ),
        osVsn     = Some( "1.0" )
      )
    }
  }

}
