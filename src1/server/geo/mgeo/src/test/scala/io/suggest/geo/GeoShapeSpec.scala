package io.suggest.geo

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 12:35
 * Description: Тесты для надмодели [[GeoShapeJvm]].
 */
class GeoShapeSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = IGeoShape

  "play-json" should "handle point shape" in {
    jsonTest( PointGs(
      MGeoPoint(lat = 10, lon = 20)
    ) )
  }

  it should "handle polygon shape" in {
    jsonTest {
      PolygonGs(
        outer = LineStringGs(Seq(
          MGeoPoint(lat = 10, lon = 20),
          MGeoPoint(lat = 30, lon = 30),
          MGeoPoint(lat = 40, lon = 40),
          MGeoPoint(lat = 10, lon = 20)
        ))
      )
    }
  }

}
