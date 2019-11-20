package io.suggest.stat.m

import io.suggest.geo.MGeoPoint
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.09.16 11:12
  * Description: Тесты для модели [[MGeoLocData].
  */
class MGeoLocDataSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MGeoLocData

  "JSON" should "support empty model" in {
    jsonTest( MGeoLocData() )
  }

  it should "support full-filled model" in {
    jsonTest {
      MGeoLocData(
        coords    = Some( MGeoPoint(lat = 33, lon = 55) ),
        accuracy  = Some( 155 ),
        town      = Some( "Engozero" ),
        country   = Some( "RU" )
      )
    }
  }

}
