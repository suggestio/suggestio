package io.suggest.model.n2.edge

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 13:09
 * Description: Тести для модели [[MNodeEdges]], которая является подмоделью [[io.suggest.model.n2.node.MNode]].
 */
class MNodeEdgesSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = MNodeEdges

  "JSON" should "handle minimal/empty model" in {
    jsonTest( MNodeEdges.empty )
    jsonTest( MNodeEdges() )
  }

  it should "handle full-filled model" in {
    jsonTest {
      val edge1 = MEdge(
        predicate = MPredicates.OwnedBy,
        nodeIds   = Set("afe432980faj3489ifvase")
      )
      val edge2 = MEdge(
        predicate = MPredicates.Receiver,
        nodeIds   = Set("f394gfkjs4e5jgis984r5g54"),
        order     = Some(5)
      )
      MNodeEdges(
        out = Seq(
          edge1,
          edge2
        )
      )
    }
  }

}
