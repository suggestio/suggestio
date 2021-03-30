package io.suggest.n2.edge

import java.time.OffsetDateTime

import io.suggest.geo.{CircleGs, Distance, MGeoPoint, MNodeGeoLevels}
import io.suggest.test.json.PlayJsonTestUtil
import org.elasticsearch.common.unit.DistanceUnit
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 12:54
 * Description: Тесты для json-модели [[MEdgeInfo]].
 */
class MEdgeInfoSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MEdgeInfo

  "JSON" should "handle empty model" in {
    jsonTest( MEdgeInfo.empty )
    jsonTest( MEdgeInfo() )
  }

  it should "handle full-filled model" in {
    jsonTest {
      MEdgeInfo(
        dateNi      = Some( OffsetDateTime.now().minusDays(3) ),
        textNi      = Some("test test 2"),
        flag        = Some(true),
        flags       = MEdgeFlagData( MEdgeFlags.AlwaysOutlined ) :: Nil,
        tags        = Set.empty + "test" + "vasya" + "123",
        geoShapes   = (
          MEdgeGeoShape(
            id     = 5,
            glevel = MNodeGeoLevels.NGL_BUILDING,
            shape  = CircleGs(
              center  = MGeoPoint(lat = 10.1, lon = 11.2),
              radiusM = Distance(10.55, DistanceUnit.KILOMETERS).meters
            )
          ) :: Nil
        ),
        geoPoints = (
          MGeoPoint(-10.1, 20.55) ::
          Nil
        )
      )
    }
  }

}
