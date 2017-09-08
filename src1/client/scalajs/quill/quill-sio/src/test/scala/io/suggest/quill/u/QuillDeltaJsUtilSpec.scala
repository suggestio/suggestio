package io.suggest.quill.u

import com.quilljs.delta.{Delta, DeltaOpAttrs}
import com.softwaremill.macwire._
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags.qd.{MQdEdgeInfo, MQdOp, MQdOpTypes, QdTag}
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.quill.QuillSioModule
import minitest._

import scala.scalajs.js

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


  test("Convert simple plain-text nothing-changing delta into qd-tag with edges") {
    val theString = "Hello world!\n\n"
    val delta1 = new Delta()
      .insert(theString)
    val hwEdgeId: EdgeUid_t = 1
    val jdTag0 = QdTag(
      ops = Seq(
        MQdOp(
          opType = MQdOpTypes.Insert,
          edgeInfo = Some( MQdEdgeInfo(hwEdgeId) )
        )
      )
    )
    val edges0 = Map(
      hwEdgeId -> MJdEditEdge(
        predicate = MPredicates.JdContent.Text,
        id        = hwEdgeId,
        text      = Some( theString )
      )
    )
    val (qdTag2, edges2) = quillDeltaJsUtil.delta2qdTag(delta1, jdTag0, edges0)

    assertEquals( qdTag2.ops.size, 1 )
    assertEquals( edges2.size, 1 )
    assert( edges2.head._2.text.contains(theString), edges2.toString )
    // Проверить совпадение id'шников
    assertEquals(
      qdTag2.ops.head.edgeInfo.get.edgeUid,
      edges2.head._1
    )
  }


  test("Covert simple plain-text, but changed, delta in qd-tag+edges") {
    val newString = "Discount 50%\nСкидки 50% \n на ВСЁ!"
    val delta1 = new Delta()
      .insert( newString )
    val hwEdgeId: EdgeUid_t = 1
    val jdTag0 = QdTag(
      ops = Seq(
        MQdOp(
          opType = MQdOpTypes.Insert,
          edgeInfo = Some( MQdEdgeInfo(hwEdgeId) )
        )
      )
    )
    val edges0 = Map(
      hwEdgeId -> MJdEditEdge(
        predicate = MPredicates.JdContent.Text,
        id        = hwEdgeId,
        text      = Some( "Please write the text here" )
      )
    )
    val (qdTag2, edges2) = quillDeltaJsUtil.delta2qdTag(delta1, jdTag0, edges0)

    assertEquals( qdTag2.ops.size, 1 )
    assertEquals( edges2.size, 1 )
    assertEquals( edges2.head._1, qdTag2.deepEdgesUidsIter.next() )
    assertEquals( edges2.head._2.text.get, newString )
  }



  test("Convert to qd-tag of minimally-bolded string like 'bla bla <b>BOLDED</b> bla bla'") {
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
    val edges0 = Map.empty[EdgeUid_t, MJdEditEdge]
    val jdTag0 = QdTag(
      ops  = Nil
    )

    val (jdTag2, edges2) = quillDeltaJsUtil.delta2qdTag(delta2, jdTag0, edges0)
    assertEquals( jdTag2.ops.size, 3 )
    assertEquals( edges2.size, 3 )

    val revDelta = quillDeltaJsUtil.qdTag2delta(jdTag2, edges2)
    assertEquals( revDelta.ops.length, 3 )

    val diffDelta = delta2.diff(revDelta)

    assert(diffDelta.ops.isEmpty)
  }

}
