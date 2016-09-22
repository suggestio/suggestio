package io.suggest.stat.m

import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 17:20
  * Description: Тесты для модели [[MCommon]].
  */
class MCommonSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MCommon

  "JSON" should "support minimally-filled model" in {
    jsonTest( MCommon() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MCommon(
        ip              = Some( "127.0.0.1" ),
        clientUid       = Some( "a3e$F$#wesrfw4efwe" ),
        uri             = Some( "/index?x=y&z=1" ),
        domain3p        = Some( "vasya.ru" ),
        isLocalClient   = Some( false ),
        gen             = Some( 134234235L )
      )
    }
  }

}
