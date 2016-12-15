package io.suggest.model.n2.geo

import io.suggest.geo.MGeoPoint
import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 18:05
 * Description: Тесты для модели [[MNodeGeo]].
 */
class MNodeGeoSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MNodeGeo

  "JSON" should "handle empty model" in {
    jsonTest( MNodeGeo.empty )
    jsonTest( MNodeGeo() )
  }

  it should "handle full-filled model" in {
    jsonTest {
      MNodeGeo(
        point = Some( MGeoPoint(22.22222, -44.44444) )
      )
    }
  }

}
