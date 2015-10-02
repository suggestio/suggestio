package io.suggest.model.n2.edge

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 12:58
 * Description: Тесты для модели [[MEdge]].
 */
class MNodeEdgeSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MEdge

  val minM ={
    MEdge(
      predicate = MPredicates.AdOwnedBy,
      nodeId    = "asf9034fi34i89fj3984u243_"
    )
  }

  "JSON" should "handle minimal model" in {
    jsonTest(minM)
  }

  it should "handle full-filled model" in {
    jsonTest {
      minM.copy(
        order = Some(4)
      )
    }
  }

}
