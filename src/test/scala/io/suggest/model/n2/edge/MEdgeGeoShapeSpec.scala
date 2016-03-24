package io.suggest.model.n2.edge

import io.suggest.model.PlayJsonTestUtil
import io.suggest.model.geo.{Distance, GeoPoint, CircleGs}
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
        id          = Some(1),
        fromUrl     = Some("https://ag.ru/nomad"),
        dateEdited  = Some(DateTime.now)
      )
    }
  }

}
