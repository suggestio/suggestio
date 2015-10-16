package io.suggest.model.geo

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
    jsonTest( PointGs(GeoPoint(10, 20)) )
  }

  it should "handle polygon shape" in {
    jsonTest {
      PolygonGs(
        outer = LineStringGs(Seq(GeoPoint(10, 20), GeoPoint(30, 30), GeoPoint(40, 40), GeoPoint(10, 20)))
      )
    }
  }

}
