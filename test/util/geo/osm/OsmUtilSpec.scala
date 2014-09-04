package util.geo.osm

import java.io.InputStream

import io.suggest.model.geo.GeoPoint
import org.scalatestplus.play._
import play.api.GlobalSettings
import play.api.test.FakeApplication

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.14 10:21
 * Description: Тесты для osm-парсеров. Тестовые файлы лежат в test/resources/util/geo/osm
 */
class OsmUtilSpec extends PlaySpec with OneAppPerSuite {

  /** Штатный Global производит долгую инициализацию, которая нам не нужен.
    * Нужен только доступ к конфигу. Ускоряем запуск: */
  override implicit lazy val app = FakeApplication(
    withGlobal = Some(new GlobalSettings() {
      override def onStart(app: play.api.Application) {
        super.onStart(app)
        println("Started dummy fake application, without Global.onStart() initialization.")
      }
    })
  )

  val TEST_FILES_DIR = "/util/geo/osm/"

  private def withFileStream(fn: String)(f: InputStream => Any): Unit = {
    val is = getClass.getResource(TEST_FILES_DIR + fn).openStream()
    try {
      f(is)
    } finally {
      is.close()
    }
  }

  "OsmUtil" must {

    "parse nodes" in {
      val nodesData = Seq(
        "node.255233500.osm.xml" -> (255233500L, GeoPoint(lat = 59.9416380, lon = 30.3098903))
      )
      nodesData foreach { case (nf, (id, gp)) =>
        withFileStream(nf) { is =>
          OsmUtil.parseElementFromStream(is, OsmElemTypes.NODE, id) mustBe OsmNode(id, gp)
        }
      }
    }


    "parse ways" in {
      case class ResultInfo(id: Long, ndLen: Int, firstNode: OsmNode, lastNode: OsmNode)
      val ways = Seq(
        "way.bolshoy-vo-part.osm.xml" -> ResultInfo(31399147L, ndLen = 9,
          firstNode = OsmNode(307016L, GeoPoint(lat = 59.9313858, lon = 30.2565811)),
          lastNode  = OsmNode(307023L, GeoPoint(lat = 59.9295036, lon = 30.2502073))
        )
      )
      ways foreach { case (wf, ri) =>
        withFileStream(wf) { is =>
          val res = OsmUtil.parseElementFromStream(is, OsmElemTypes.WAY, ri.id)
          res.id  mustBe  ri.id
          assert( res.isInstanceOf[OsmWay] )
          val resWay = res.asInstanceOf[OsmWay]
          resWay.nodesOrdered.size  mustBe  ri.ndLen
          resWay.nodesOrdered.head  mustBe  ri.firstNode
          resWay.nodesOrdered.last  mustBe  ri.lastNode
        }
      }
    }


    "parse relations" in {
      case class ResultInfo(id: Long, membersCount: Int, firstBorderMemberId: Long, lastBorderMemberId: Long)
      val rels: Seq[(String, ResultInfo)] = Seq(
        "rel.vaska.osm.xml" -> ResultInfo(1114252L, membersCount = 24,
          firstBorderMemberId = 71338507L,
          lastBorderMemberId = 71346364L
        ),
        "rel.vyborgsky.spb.osm.xml" -> ResultInfo(1114354, 107,
          firstBorderMemberId = 178770468,
          lastBorderMemberId = 100599524L
        ),
        "rel.spb.osm.xml" -> ResultInfo(337422L, 183,
          firstBorderMemberId = 197668703L,
          lastBorderMemberId = 175195149L
        ),
        "rel.spb.kirovsky-rajon.osm.xml" -> ResultInfo(369514L, 23,
          firstBorderMemberId = 31396996L,
          lastBorderMemberId = 159348605L
        )
      )
      rels foreach { case (rf, ri) =>
        withFileStream(rf) { is =>
          val res = OsmUtil.parseElementFromStream(is, OsmElemTypes.RELATION, ri.id)
          res.id mustBe ri.id
          assert( res.isInstanceOf[OsmRelation] )
          val rel = res.asInstanceOf[OsmRelation]
          rel.members.size  mustBe  ri.membersCount
          val bms = rel.borderMembers.toSeq
          bms.head.obj.id  mustBe  ri.firstBorderMemberId
          bms.last.obj.id  mustBe  ri.lastBorderMemberId
          res.toGeoShape  // Не должно быть экзепшена
          // TODO Нужно проверять корректность линий
        }
      }
    }

  }

}
