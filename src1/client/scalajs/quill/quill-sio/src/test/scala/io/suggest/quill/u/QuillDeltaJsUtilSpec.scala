package io.suggest.quill.u

import com.quilljs.delta.{Delta, DeltaOpAttrs}
import com.softwaremill.macwire._
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.JdTag
import io.suggest.jd.tags.JdTag.Implicits._
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.quill.QuillSioModule
import minitest._

import scala.scalajs.js
import scalaz.Tree

// TODO Есть проблемы со сложностью типов в Delta.insert(). Раздолбить insert API на несколько методов + JSName?
import scala.language.existentials

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.17 11:51
  * Description: Жирные тесты для js-утили работы с Quill Delta форматом.
  */
object QuillDeltaJsUtilSpec extends SimpleTestSuite {

  /** Отработать di для получения инстанса [[QuillDeltaJsUtil]]. */
  private val quillDeltaJsUtil = wire[QuillSioModule].quillDeltaJsUtil

  private def coords = MCoords2di(10, 20)

  test("Convert simple plain-text nothing-changing delta into qd-tag with edges") {
    val theString = "Hello world!\n\n"
    val delta1 = new Delta()
      .insert(theString)
    val hwEdgeId: EdgeUid_t = 1

    val jdTag0 = JdTag.edgeQdTree( hwEdgeId, coords )

    val edges0 = Map(
      hwEdgeId -> MEdgeDataJs(
        jdEdge = MJdEditEdge(
          predicate = MPredicates.JdContent.Text,
          id        = hwEdgeId,
          text      = Some( theString )
        )
      )
    )
    val (qdTagTree2, edges2) = quillDeltaJsUtil.delta2qdTag(delta1, jdTag0, edges0)

    val qdOps2 = qdTagTree2.qdOpsIter.toSeq

    assertEquals( qdOps2.size, 1 )
    assertEquals( edges2.size, 1 )
    assert( edges2.head._2.jdEdge.text.contains(theString), edges2.toString )
    // Проверить совпадение id'шников
    assertEquals(
      qdOps2.head.edgeInfo.get.edgeUid,
      edges2.head._1
    )
  }


  test("Covert simple plain-text, but changed, delta in qd-tag+edges") {
    val newString = "Discount 50%\nСкидки 50% \n на ВСЁ!"
    val delta1 = new Delta()
      .insert( newString )
    val hwEdgeId: EdgeUid_t = 1
    val jdTag0 = JdTag.edgeQdTree(hwEdgeId, coords)
    val edges0 = Map(
      hwEdgeId -> MEdgeDataJs(
        jdEdge = MJdEditEdge(
          predicate = MPredicates.JdContent.Text,
          id        = hwEdgeId,
          text      = Some( "Please write the text here" )
        )
      )
    )
    val (qdTagTree2, edges1) = quillDeltaJsUtil.delta2qdTag(delta1, jdTag0, edges0)
    val edges2 = quillDeltaJsUtil.purgeUnusedEdges( qdTagTree2, edges1 )

    assertEquals( qdTagTree2.qdOpsIter.size, 1 )
    assertEquals( edges2.size, 1 )
    assertEquals( edges2.head._1, qdTagTree2.deepEdgesUidsIter.next() )
    assertEquals( edges2.head._2.jdEdge.text.get, newString )
  }



  test("Convert to qd-tag of minimally-bolded string like 'bla bla <b>BOLDED</b> bla bla' (absolutely empty document0)") {
    // Пример нерабочей дельты взят прямо из редактора.
    // {"ops":[
    //   {"insert":"lorem ipsum und uber "},
    //   {"attributes":{"bold":true},"insert":"blochHeight"},
    //   {"insert":" wr2 34t\n"}
    // ]}
    val strBefore = "lorem ipsum und uber "
    val strBolded = "blochHeight"
    val strAfter  = " wr2 34t\n"

    val delta2 = new Delta()
      .insert( strBefore )
      .insert( strBolded,
        attributes = {
          val dAttrs = js.Object().asInstanceOf[DeltaOpAttrs]
          dAttrs.bold = js.defined( true )
          dAttrs
        }
      )
      .insert( strAfter )

    // Пусть исходный документ будет пустым. Для чистоты эксперимента.
    val edges0 = Map.empty[EdgeUid_t, MEdgeDataJs]
    val jdTag0 = Tree.Leaf(
      JdTag.qd( coords )
    )

    val (qdTagTree2, edges2) = quillDeltaJsUtil.delta2qdTag(delta2, jdTag0, edges0)
    assertEquals( qdTagTree2.qdOpsIter.size, 3 )
    assertEquals( edges2.size, 3 )

    val revDelta = quillDeltaJsUtil.qdTag2delta(qdTagTree2, edges2)
    assertEquals( revDelta.ops.length, 3 )

    val diffDelta = delta2.diff(revDelta)

    assert(diffDelta.ops.isEmpty)
  }


  test("Convert to qd-tag of minimally-bolded string like 'bla bla <b>BOLDED</b> bla bla' (non-empty document0)") {
    // {"ops":[
    //   {"insert":"lorem ipsum und uber "},
    //   {"attributes":{"bold":true},"insert":"blochHeight"},
    //   {"insert":" wr2 34t\n"}
    // ]}
    val strBefore = "lorem ipsum und uber "
    val strBolded = "blochHeight"
    val strAfter  = " wr2 34t\n"

    val delta2 = new Delta()
      .insert( strBefore )
      .insert( strBolded,
        attributes = {
          val dAttrs = js.Object().asInstanceOf[DeltaOpAttrs]
          dAttrs.bold = js.defined( true )
          dAttrs
        }
      )
      .insert( strAfter )

    // В эджах какой-то мусор. Но он не должен потеряться.
    val edges0 = Map[EdgeUid_t, MEdgeDataJs](
      1 -> MEdgeDataJs( MJdEditEdge(MPredicates.Bg, 1, url = Some("blob:asdasdasdsda")) ),
      3 -> MEdgeDataJs( MJdEditEdge(MPredicates.Bg, 3, url = Some("blob:645v-56h65y5665h56")) ),
      4 -> MEdgeDataJs( MJdEditEdge(MPredicates.JdContent.Video, 4, url = Some("https://youtu.be/art42364")) )
    )

    val jdTag0 = Tree.Leaf(
      JdTag.qd( coords )
    )

    val (qdTagTree2, edges2) = quillDeltaJsUtil.delta2qdTag(delta2, jdTag0, edges0)
    assertEquals( qdTagTree2.qdOpsIter.size, 3 )
    assertEquals( edges2.size, 6 )

    val revDelta = quillDeltaJsUtil.qdTag2delta(qdTagTree2, edges2)
    assertEquals( revDelta.ops.length, 3 )

    val diffDelta = delta2.diff(revDelta)

    assert(diffDelta.ops.isEmpty)
  }


  test("Convert to qd-tag of minimally-bolded string like 'bla bla <b>BOLDED</b> bla bla' (non-empty document0 with garbadge in edges0)") {
    // {"ops":[
    //   {"insert":"lorem ipsum und uber "},
    //   {"attributes":{"bold":true},"insert":"blochHeight"},
    //   {"insert":" wr2 34t\n"}
    // ]}
    val strBefore = "lorem ipsum und uber "
    val strBolded = "blochHeight"
    val strAfter  = " wr2 34t\n"

    val delta2 = new Delta()
      .insert( strBefore )
      .insert( strBolded,
        attributes = {
          val dAttrs = js.Object().asInstanceOf[DeltaOpAttrs]
          dAttrs.bold = js.defined( true )
          dAttrs
        }
      )
      .insert( strAfter )

    // Пусть исходные эджи содержат только мусор:
    val edges0 = Map[EdgeUid_t, MEdgeDataJs](
      1 -> MEdgeDataJs( MJdEditEdge(MPredicates.Bg, 1, url = Some("blob:asdasdasdsda")) ),
      3 -> MEdgeDataJs( MJdEditEdge(MPredicates.Bg, 3, url = Some("blob:645v-56h65y5665h56")) ),
      4 -> MEdgeDataJs( MJdEditEdge(MPredicates.JdContent.Video, 4, url = Some("https://youtu.be/art42364")) ),
      0 -> MEdgeDataJs( MJdEditEdge(MPredicates.JdContent.Text, 0, text = Some("asdasd")) )
    )

    val jdTag0 = JdTag.edgeQdTree(0, coords)

    val (qdTagTree2, edges1) = quillDeltaJsUtil.delta2qdTag(delta2, jdTag0, edges0)
    val edges2 = quillDeltaJsUtil.purgeUnusedEdges( qdTagTree2, edges1 )
    assertEquals( qdTagTree2.qdOpsIter.size, 3 )
    assertEquals( edges2.size, 3 )

    val revDelta = quillDeltaJsUtil.qdTag2delta(qdTagTree2, edges2)
    assertEquals( revDelta.ops.length, 3 )

    val diffDelta = delta2.diff(revDelta)

    assert(diffDelta.ops.isEmpty)
  }


  test("~Complex DELTA => QdTag, non-empty document0 with mostly unrelated edges0") {
    // {"ops":[
    //   {"insert":"lorem ipsum und uber "},
    //   {"attributes":{"bold":true},"insert":"blochHeight"},
    //   {"insert":" wr2 34t\n"}
    // ]}
    val strBefore = "lorem ipsum und uber "
    val strBolded = "blochHeight"
    val strAfter  = " wr2 34t\n"

    val delta2 = new Delta()
      .insert( strBefore )
      .insert( strBolded,
        attributes = {
          val dAttrs = js.Object().asInstanceOf[DeltaOpAttrs]
          dAttrs.bold = js.defined( true )
          dAttrs
        }
      )
      .insert( strAfter )

    // Пусть исходные эджи содержат только что-то, отсосящиеся к другим частям документа:
    val edges0 = Map[EdgeUid_t, MEdgeDataJs](
      1 -> MEdgeDataJs( MJdEditEdge(MPredicates.Bg, 1, url = Some("blob:asdasdasdsda")) ),
      3 -> MEdgeDataJs( MJdEditEdge(MPredicates.Bg, 3, url = Some("blob:645v-56h65y5665h56")) ),
      4 -> MEdgeDataJs( MJdEditEdge(MPredicates.JdContent.Video, 4, url = Some("https://youtu.be/art42364")) ),
      0 -> MEdgeDataJs( MJdEditEdge(MPredicates.JdContent.Text, 0, text = Some("asdasd")) ),
      5 -> MEdgeDataJs( MJdEditEdge(MPredicates.JdContent.Text, 5, text = Some(strBefore)) )
    )

    val jdTag0 = JdTag.edgeQdTree(0, coords)

    val (jdTag2, edges2) = quillDeltaJsUtil.delta2qdTag(delta2, jdTag0, edges0)
    assertEquals( jdTag2.qdOpsIter.size, 3 )
    assertEquals( edges2.size, 7 )

    val revDelta = quillDeltaJsUtil.qdTag2delta(jdTag2, edges2)
    assertEquals( revDelta.ops.length, 3 )

    val diffDelta = delta2.diff(revDelta)

    assert(diffDelta.ops.isEmpty)
  }

}
