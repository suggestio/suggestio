package io.suggest.model.n2.node

import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.MNodeMeta
import io.suggest.model.n2.tag.vertex.{MTagFace, MTagVertex}
import io.suggest.ym.model.common.AdnRights
import org.scalatest.Matchers._
import org.scalatest._

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

  private def _mnc: MNodeCommon = {
    MNodeCommon(
      ntype = MNodeTypes.AdnNode,
      isDependent = false
    )
  }

  classOf[MNode].getSimpleName should "minimally handle JSON serialize/deserialize" in {
    t(MNode(
      common = _mnc
    ))
  }

  it should "handle JSON for filled MNode" in {
    t {
      MNode(
        common = _mnc,
        meta = MNodeMeta(
          nameOpt = Some("the name, 121 !"),
          hiddenDescr = Some("some hidden descr!@312#!@Fsrf erfsa erfare\n\n\n\r\n 324sASf asdd")
        ),
        extras = MNodeExtras(
          tag = Some(MTagVertex(
            faces = MTagFace.faces2map(Seq(
              MTagFace("один"), MTagFace("два"), MTagFace("три")
            ))
          )),
          adn = Some(MAdnExtra(
            rights = Set(AdnRights.PRODUCER)
          ))
        )
      )
    }
  }

}
