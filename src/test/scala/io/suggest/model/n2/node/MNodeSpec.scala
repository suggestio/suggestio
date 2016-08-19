package io.suggest.model.n2.node

import io.suggest.model.MockedEsSn
import io.suggest.model.geo.GeoPoint
import io.suggest.model.n2.ad.MNodeAd
import io.suggest.model.n2.ad.blk.BlockMeta
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.model.n2.geo.MNodeGeo
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.ym.model.common.AdnRights
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.15 14:38
 * Description: Тесты для мегамодели [[MNode]].
 */
class MNodeSpec extends PlaySpec with OneAppPerSuite with MockedEsSn {

  private lazy val mNodes = app.injector.instanceOf[MNodes]

  private def t(mn: MNode): Unit = {
    import mNodes.mockPlayDocRespEv
    mNodes.deserializeOne2(mn)  shouldBe  mn
  }

  private def _mnc: MNodeCommon = {
    MNodeCommon(
      ntype = MNodeTypes.AdnNode,
      isDependent = false
    )
  }


  classOf[MNode].getSimpleName must {
    "minimally handle JSON serialize/deserialize" in {
      t(MNode(
        common = _mnc,
        meta = MMeta(MBasicMeta())
      ))
    }

    "handle JSON for filled MNode" in {
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
            adn = Some(MAdnExtra(
              rights = Set(AdnRights.PRODUCER)
            ))
          ),
          edges = MNodeEdges(
            out = {
              MNodeEdges.edgesToMap(
                MEdge(
                  predicate = MPredicates.Logo,
                  nodeIds = Set("fa4f493wfr3420f3904__4")
                )
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

}
