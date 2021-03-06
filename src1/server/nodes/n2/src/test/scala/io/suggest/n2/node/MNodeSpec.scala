package io.suggest.n2.node

import io.suggest.adn.MAdnRights
import io.suggest.geo.{CircleGs, MGeoPoint, MNodeGeoLevels}
import io.suggest.model.MockedEsSn
import io.suggest.n2.edge._
import io.suggest.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import org.scalatest.matchers.should.Matchers._
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
    import mNodes.Implicits.mockPlayDocRespEv
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
              rights = Set(MAdnRights.PRODUCER)
            ))
          ),
          edges = MNodeEdges(
            out = {
              MNodeEdges.edgesToMap(
                MEdge(
                  predicate = MPredicates.AdvGeoPlace,
                  nodeIds = Set("fa4f493wfr3420f3904__4"),
                  info = MEdgeInfo(
                    flag = Some(true),
                    flags =
                      MEdgeFlagData( MEdgeFlags.AlwaysOutlined ) ::
                      MEdgeFlagData(MEdgeFlags.AlwaysOpened) ::
                      Nil,
                    geoShapes =
                      MEdgeGeoShape(
                        id = 1,
                        glevel = MNodeGeoLevels.NGL_BUILDING,
                        shape  = CircleGs(
                          center  = MGeoPoint(lat = 10, lon = 15),
                          radiusM = 2000
                        )
                      ) ::
                      Nil,
                  )
                )
              )
            }
          )
        )
      }
    }

  }

}
