package io.suggest.model.n2.edge

import io.suggest.model.PlayJsonTestUtil
import io.suggest.ym.model.common.SinkShowLevels
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
        dynImgArgs = Some("afaW?Fa234f9843w5f=63.,h56423&&#456"),
        sls = Set( SinkShowLevels.GEO_CATS_SL, SinkShowLevels.GEO_PRODUCER_SL )
      )
    }
  }

}
