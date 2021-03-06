package io.suggest.n2.edge

import io.suggest.test.json.PlayJsonTestUtil
import org.scalatest.flatspec.AnyFlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 12:24
 * Description: Тесты для json-модели [[MEdge]].
 */
class MEdgeSpec extends AnyFlatSpec with PlayJsonTestUtil {

  override type T = MEdge

  "JSON" should "support minimal model" in {
    jsonTest {
      MEdge(
        predicate = MPredicates.OwnedBy,
        nodeIds   = Set("ADFa3498fj39845j324f")
      )
    }
  }

  it should "support full-filled model" in {
    jsonTest {
      MEdge(
        predicate = MPredicates.ModeratedBy,
        nodeIds   = Set("avf90fk43a90fk34af34f"),
        order     = Some(5),
        info      = MEdgeInfo(
          textNi = Some("ASDASDASD")
        )
      )
    }
  }

}
