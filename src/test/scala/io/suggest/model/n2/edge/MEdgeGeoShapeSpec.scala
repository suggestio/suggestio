package io.suggest.model.n2.edge

import io.suggest.model.PlayJsonTestUtil
import io.suggest.model.geo._
import io.suggest.ym.model.NodeGeoLevels
import org.elasticsearch.common.unit.DistanceUnit
import org.joda.time.DateTime
import org.scalatest.FlatSpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.03.16 16:02
  * Description: Тесты для модели геошейпов внутри эджей.
  */
class MEdgeGeoShapeSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MEdgeGeoShape

  private def _gs0 = {
    MEdgeGeoShape(
      id     = 1,
      glevel = NodeGeoLevels.NGL_BUILDING,
      shape  = CircleGs(
        center = GeoPoint(10.1, 11.2),
        radius = Distance(10.55, DistanceUnit.KILOMETERS)
      )
    )
  }

  "JSON" should "handle minimal model" in {
    jsonTest(_gs0)
  }

  it should "handle full-filled model" in {
    jsonTest {
      _gs0.copy(
        id          = 2,
        fromUrl     = Some("https://ag.ru/nomad"),
        dateEdited  = Some(DateTime.now)
      )
    }
  }

  it should "handle other full-filled model" in {
    jsonTest {
      MEdgeGeoShape(
        id     = 10,
        glevel = NodeGeoLevels.NGL_TOWN,
        shape  = PolygonGs(
          outer = LineStringGs(Seq(
            GeoPoint(10, 10), GeoPoint(20, 20), GeoPoint(30, 30), GeoPoint(0, 30), GeoPoint(10, 10)
          ))
        ),
        fromUrl = Some("https://x.com/asd.jpg"),
        dateEdited = Some( new DateTime(2015, 1, 2, 3, 4, 5, 6) )
      )
    }
  }

}
