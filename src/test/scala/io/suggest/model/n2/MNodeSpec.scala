package io.suggest.model.n2

import io.suggest.model.n2.node.MNode
import io.suggest.model.n2.tag.MNodeTagInfo
import io.suggest.model.n2.tag.vertex.{MTagFace, MTagVertex}
import org.scalatest._, Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.15 14:38
 * Description: Тесты для мегамодели [[MNode]].
 */
class MNodeSpec extends FlatSpec {

  private def t(mn: MNode): Unit = {
    MNode.deserializeOne2(mn)  shouldBe  mn
  }

  classOf[MNode].getSimpleName should "minimally handle JSON serialize/deserialize" in {
    t( MNode() )
  }

  it should "handle JSON for filled MNode" in {
    t {
      MNode(
        tag = MNodeTagInfo(
          vertex = Some(MTagVertex(
            faces = MTagFace.faces2map(Seq(
              MTagFace("один"), MTagFace("два"), MTagFace("три")
            ))
          ))
        )
      )
    }
  }

}
