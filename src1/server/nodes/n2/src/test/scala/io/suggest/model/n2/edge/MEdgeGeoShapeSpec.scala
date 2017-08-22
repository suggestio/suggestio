package io.suggest.model.n2.edge

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}

import io.suggest.geo._
import io.suggest.test.json.PlayJsonTestUtil
import MNodeGeoLevels
import org.elasticsearch.common.unit.DistanceUnit
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
      glevel = MNodeGeoLevels.NGL_BUILDING,
      shape  = CircleGs(
        center  = MGeoPoint(lat = 10.1, lon = 11.2),
        radiusM = Distance(10.55, DistanceUnit.KILOMETERS).meters
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
        dateEdited  = Some( OffsetDateTime.now() )
      )
    }
  }

  it should "handle other full-filled model" in {
    jsonTest {
      MEdgeGeoShape(
        id     = 10,
        glevel = MNodeGeoLevels.NGL_TOWN,
        shape  = PolygonGs(
          outer = LineStringGs(Seq(
            MGeoPoint(lat = 10, lon = 10),
            MGeoPoint(lat = 20, lon = 20),
            MGeoPoint(lat = 30, lon = 30),
            MGeoPoint(lat = 0,  lon = 30),
            MGeoPoint(lat = 10, lon = 10)
          ))
        ),
        fromUrl = Some("https://x.com/asd.jpg"),
        dateEdited = Some( LocalDateTime.of(2015, 1, 2, 3, 4, 5, 6).atOffset( ZoneOffset.UTC ) )
      )
    }
  }

}
