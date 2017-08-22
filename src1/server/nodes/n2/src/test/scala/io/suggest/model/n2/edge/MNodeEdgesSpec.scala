package io.suggest.model.n2.edge

import java.time.OffsetDateTime

import io.suggest.geo.{CircleGs, MGeoPoint, MNodeGeoLevels}
import io.suggest.test.json.PlayJsonTestUtil
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

      val edge3 = MEdge(
        predicate = MPredicates.NodeLocation,
        info = MEdgeInfo(
          dynImgArgs = Some("easdasd"),
          dateNi = Some( OffsetDateTime.now().minusDays(1) ),
          commentNi = Some("asdas asd as a#$!#@$ds'ad''''!"),
          flag = Some(true),
          tags    = Set("asd", "bbb"),
          geoShapes = List(
            MEdgeGeoShape(
              id = 1,
              glevel = MNodeGeoLevels.NGL_BUILDING,
              shape  = CircleGs(
                center  = MGeoPoint(lat = 10, lon = 15),
                radiusM = 2000
              ),
              fromUrl = Some("http://asdasdasd/.df"),
              dateEdited = Some( OffsetDateTime.now() )
            )
          )
        )
      )

      MNodeEdges(
        out = Seq(
          edge1,
          edge2,
          edge3
        )
      )
    }
  }

}
