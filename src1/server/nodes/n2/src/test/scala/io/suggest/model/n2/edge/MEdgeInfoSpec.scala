package io.suggest.model.n2.edge

import java.time.OffsetDateTime

import io.suggest.geo.{CircleGs, Distance, MGeoPoint}
import io.suggest.test.json.PlayJsonTestUtil
import io.suggest.ym.model.NodeGeoLevels
import org.elasticsearch.common.unit.DistanceUnit
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 12:54
 * Description: Тесты для json-модели [[MEdgeInfo]].
 */
class MEdgeInfoSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MEdgeInfo

  "JSON" should "handle empty model" in {
    jsonTest( MEdgeInfo.empty )
    jsonTest( MEdgeInfo() )
  }

  it should "handle full-filled model" in {
    jsonTest {
      MEdgeInfo(
        dynImgArgs  = Some("afaW?Fa234f9843w5f=63.,h56423&&#456"),
        dateNi      = Some( OffsetDateTime.now().minusDays(3) ),
        commentNi   = Some("test test 2"),
        flag        = Some(true),
        itemIds     = Set(13242134L),
        tags        = Set("test", "vasya", "123"),
        geoShapes   = List(
          MEdgeGeoShape(
            id     = 5,
            glevel = NodeGeoLevels.NGL_BUILDING,
            shape  = CircleGs(
              center = MGeoPoint(lat = 10.1, lon = 11.2),
              radius = Distance(10.55, DistanceUnit.KILOMETERS)
            )
          )
        )
      )
    }
  }

}
