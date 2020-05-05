package io.suggest.stat.m

import org.scalatest.flatspec.AnyFlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 17:20
  * Description: Тесты для модели [[MCommonStat]].
  */
class MCommonStatSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MCommonStat

  "JSON" should "support minimally-filled model" in {
    jsonTest {
      MCommonStat(
        components = MComponents.Sc :: Nil
      )
    }
  }

  it should "support full-filled model" in {
    jsonTest {
      MCommonStat(
        components      = Seq(MComponents.Sc),
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
