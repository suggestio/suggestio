package io.suggest.model.geo

import io.suggest.geo.MGeoPoint
import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 12:35
 * Description: Тесты для надмодели [[GeoShape]].
 */
class GeoShapeSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = GeoShape

  "play-json" should "handle point shape" in {
    jsonTest( PointGs(MGeoPoint(10, 20)) )
  }

  it should "handle polygon shape" in {
    jsonTest {
      PolygonGs(
        outer = LineStringGs(Seq(MGeoPoint(10, 20), MGeoPoint(30, 30), MGeoPoint(40, 40), MGeoPoint(10, 20)))
      )
    }
  }

}
