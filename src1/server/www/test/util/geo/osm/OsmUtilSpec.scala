package util.geo.osm

import functional.{OneAppPerSuiteNoGlobalStart, WithInputStream}
import io.suggest.geo._
import org.scalatestplus.play._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.14 10:21
 * Description: Тесты для osm-парсеров. Тестовые файлы лежат в test/resources/util/geo/osm
 */
class OsmUtilSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart with WithInputStream {

  override val RES_DIR = "/util/geo/osm/"

  case class RelResultInfo(id: Long, membersCount: Int, firstBorderMemberId: Long, lastBorderMemberId: Long,
                           gsType: GsType, allMemberIds: Seq[Long] = Nil)

  private def testRel(rf: String, ri: RelResultInfo): Unit = {
    withFileStream(rf) { is =>
      val res = OsmUtil.parseElementFromStream(is, OsmElemTypes.RELATION, ri.id)
      res.id mustBe ri.id
      assert( res.isInstanceOf[OsmRelation] )
      val rel = res.asInstanceOf[OsmRelation]
      rel.members.size  mustBe  ri.membersCount
      val bms = rel.borderMembers.toSeq
      bms.head.obj.id  mustBe  ri.firstBorderMemberId
      bms.last.obj.id  mustBe  ri.lastBorderMemberId
      val gs = res.toGeoShape
      Json.toJson( gs )   // Не должно быть экзепшена
      if (ri.allMemberIds.nonEmpty) {
        // Пройтись по всему списку member'ов
        for(i <- rel.members.indices) {
          rel.members(i).obj.id  mustBe  ri.allMemberIds(i)
        }
      }
      // TODO Нужно проверять корректность линий: порядок точек внутри way'я может быть как прямой, так и обратный
      // по отношению к соседним путям.
    }
  }


  case class WayResultInfo(id: Long, ndLen: Int, firstNode: OsmNode, lastNode: OsmNode)
  private def testWay(wf: String, ri: WayResultInfo): Unit = {
    withFileStream(wf) { is =>
      val res = OsmUtil.parseElementFromStream(is, OsmElemTypes.WAY, ri.id)
      res.id  mustBe  ri.id
      assert( res.isInstanceOf[OsmWay] )
      val resWay = res.asInstanceOf[OsmWay]
      resWay.nodesOrdered.size  mustBe  ri.ndLen
      resWay.nodesOrdered.head  mustBe  ri.firstNode
      resWay.nodesOrdered.last  mustBe  ri.lastNode
      val gs = resWay.toGeoShape
      assert( gs.isInstanceOf[LineStringGs] )
      val lsgs = gs.asInstanceOf[LineStringGs]
      lsgs.coords  mustBe  resWay.nodesOrdered.map(_.gp)
    }
  }


  "OsmUtil" must {

    "parse nodes" in {
      val nodesData = Seq(
        "node.255233500.osm.xml" -> (255233500L, MGeoPoint(lat = 59.9416380, lon = 30.3098903))
      )
      nodesData foreach { case (nf, (id, gp)) =>
        withFileStream(nf) { is =>
          OsmUtil.parseElementFromStream(is, OsmElemTypes.NODE, id) mustBe OsmNode(id, gp)
        }
      }
    }


    "testWay(way.bolshoy-vo-part.osm.xml)" in {
      testWay(
        "way.bolshoy-vo-part.osm.xml",
        WayResultInfo(31399147L, ndLen = 9,
          firstNode = OsmNode(307016L, MGeoPoint(lat = 59.9313858, lon = 30.2565811)),
          lastNode  = OsmNode(307023L, MGeoPoint(lat = 59.9295036, lon = 30.2502073))
        )
      )
    }

    "testRel(spb.vaska)" in {
      testRel("rel.vaska.osm.xml", RelResultInfo(1114252L, membersCount = 24,
        firstBorderMemberId = 71338507L,
        lastBorderMemberId = 71346364L,
        gsType = GsTypes.Polygon
      ))
    }

    "testRel(rel.vyborgsky.spb)" in {
      testRel("rel.vyborgsky.spb.osm.xml", RelResultInfo(1114354, 107,
        firstBorderMemberId = 178770468,
        lastBorderMemberId = 100599524L,
        gsType = GsTypes.Polygon
      ))
    }

    "testRel(rel.spb.osm.xml)" in {
      testRel("rel.spb.osm.xml", RelResultInfo(337422L, 183,
        firstBorderMemberId = 197668703L,
        lastBorderMemberId = 175195149L,
        gsType = GsTypes.Polygon
      ))
    }

    "testRel(rel.spb.kirovsky)" in {
      testRel(
        "rel.spb.kirovsky-rajon.osm.xml", RelResultInfo(369514L, 23,
          firstBorderMemberId = 31396996L,
          lastBorderMemberId = 159348605L,
          gsType = GsTypes.Polygon,
          allMemberIds = Seq(31396996L, 182611220L, 37669206L, 71338511L, 79570062L, 46835382L, 46835375L,
            79522204L, 79522201L, 46896188L, 46896180L, 46835385L, 45205337L, 79522192L, 79522199L, 45205338L,
            79522198L, 79522202L, 79522193L, 45273634L, 264454722L, 264454721L, 159348605L
          )
        )
      )
    }

    // TODO Нужны тесты для OsmRel.directWays()
  }

}
