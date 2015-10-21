package io.suggest.model.n2.geo

import io.suggest.model.PlayJsonTestUtil
import io.suggest.model.geo.{GeoPoint, LineStringGs, PolygonGs}
import io.suggest.ym.model.NodeGeoLevels
import org.joda.time.DateTime
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 16:22
 * Description: Тесты для субмодели [[MGeoShape]].
 */
class MGeoShapeSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MGeoShape

  "JSON" should "support model" in {
    jsonTest {
      MGeoShape(
        id     = 10,
        glevel = NodeGeoLevels.NGL_TOWN,
        shape  = PolygonGs(
          outer = LineStringGs(Seq(
            GeoPoint(10, 10), GeoPoint(20, 20), GeoPoint(30, 30), GeoPoint(0, 30), GeoPoint(10, 10)
          ))
        ),
        fromUrl = Some("https://x.com/asd.jpg"),
        dateEdited = new DateTime(2015, 1, 2, 3, 4, 5, 6)
      )
    }
  }

}
