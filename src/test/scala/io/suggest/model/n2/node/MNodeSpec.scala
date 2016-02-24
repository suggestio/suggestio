package io.suggest.model.n2.node

import io.suggest.model.geo.GeoPoint
import io.suggest.model.n2.ad.MNodeAd
import io.suggest.model.n2.ad.blk.BlockMeta
import io.suggest.model.n2.edge.{MPredicates, MEdge, MNodeEdges}
import io.suggest.model.n2.extra.tag.{MTagExtra, MTagFace}
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.model.n2.geo.MNodeGeo
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
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
      common  = _mnc,
      meta    = MMeta( MBasicMeta() )
    ))
  }

  it should "handle JSON for filled MNode" in {
    t {
      MNode(
        common = _mnc,
        meta = MMeta(
          basic = MBasicMeta(
            nameOpt = Some("the name, 121 !"),
            hiddenDescr = Some("some hidden descr!@312#!@Fsrf erfsa erfare\n\n\n\r\n 324sASf asdd")
          )
        ),
        extras = MNodeExtras(
          tag = Some(MTagExtra(
            faces = MTagFace.faces2map(
              MTagFace("один"), MTagFace("два"), MTagFace("три")
            )
          )),
          adn = Some(MAdnExtra(
            rights = Set(AdnRights.PRODUCER)
          ))
        ),
        edges = MNodeEdges(
          out = {
            MNodeEdges.edgesToMap(
              MEdge(MPredicates.Logo, "fa4f493wfr3420f3904__4")
            )
          }
        ),
        geo = MNodeGeo(
          point = Some(GeoPoint(10.0, 33.2))
        ),
        ad = MNodeAd(
          blockMeta = Some(BlockMeta.DEFAULT)
        )
      )
    }
  }

}
