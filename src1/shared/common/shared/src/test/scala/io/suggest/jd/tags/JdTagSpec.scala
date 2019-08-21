package io.suggest.jd.tags

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths, MBlockExpandModes}
import io.suggest.common.geom.coord.MCoords2di
import minitest._
import play.api.libs.json.{Json, OFormat}
import io.suggest.scalaz.ZTreeUtil._
import scalaz.{Show, Tree}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 21:52
  * Description: Тесты для древовидной рекурсивной модели [[JdTag]].
  */
// Надо как-то это разрулить наверное...
object JdTagSpec extends SimpleTestSuite {

  /** Тестирование сериализации и десериализации переданного объекта.
    *
    * @param v Тестируемое значение.
    * @param jsonFmt play JSON formatter.
    * @tparam T Тип тестируемого значения.
    */
  private def _writeReadMatchTest[T <: JdTag](v: Tree[T])(implicit jsonFmt: OFormat[T], show: Show[T]): Unit = {
    val treeFmt = implicitly[OFormat[Tree[T]]]
    val jsonStr = treeFmt.writes(v).toString()
    val jsRes   = treeFmt.reads( Json.parse( jsonStr ) )
    assert( jsRes.isSuccess, jsRes.toString )
    val v2 = jsRes.get
    // IEqualsEq запрещает сравнивать разные инстансы, даже с одинаковыми полями. Поэтому сравниваем string-рендеры деревьев.
    assertEquals(
      v2.drawTree,
      v.drawTree
    )
  }

  private val bm300x140 = BlockMeta(BlockWidths.NORMAL, BlockHeights.H140, expandMode = None)
  private val bm300x300 = BlockMeta(BlockWidths.NORMAL, BlockHeights.H300, expandMode = Some( MBlockExpandModes.Wide ))


  private def coord1 = MCoords2di(10, 20)
  private def coord2 = MCoords2di(40, 40)
  private def coord3 = MCoords2di(111, 10)


  test("JSON: Empty document") {
    val doc = Tree.Leaf(
      JdTag.document
    )
    _writeReadMatchTest( doc )
  }


  test("JSON: Simple Document( Strip(PlainPayload()) )") {
    val doc = Tree.Node(
      JdTag.document,
      Stream(
        Tree.Node(
          JdTag.strip( bm300x140 ),
          Stream(
            JdTag.edgeQdTree(1, coord1)
          )
        )
      )
    )
    _writeReadMatchTest( doc )
  }


  test("JSON: Document with two strips, each with several children") {
    val doc = Tree.Node(
      JdTag.document,
      Stream(
        Tree.Node(
          JdTag.strip( bm300x140 ),
          Stream(
            JdTag.edgeQdTree(2, coord1),
            JdTag.edgeQdTree(4, coord2)
          )
        ),
        Tree.Node(
          JdTag.strip( bm300x300 ),
          Stream(
            JdTag.edgeQdTree(5, coord1),
            JdTag.edgeQdTree(1, coord2),
            JdTag.edgeQdTree(2, coord3)
          )
        )
      )
    )
    _writeReadMatchTest(doc)
  }


  test("JSON: 3-level document tree with inner children") {
    val jdTag_p1_topLeft = JdTag.props1 composeLens MJdtProps1.topLeft
    val doc = Tree.Node(
      JdTag.document,
      Stream(
        Tree.Node(
          JdTag.strip( bm300x140 ),
          Stream(
            JdTag.edgeQdTree(2, coord1)
              .loc
              .modifyLabel {
                jdTag_p1_topLeft set Some(MCoords2di(10, 20))
              }
              .toTree,
            //_picture(555),
            JdTag.edgeQdTree(4, coord2)
          )
        ),
        Tree.Node(
          JdTag.strip( bm300x300 ),
          Stream(
            JdTag.edgeQdTree(5, coord3)
              .loc
              .modifyLabel {
                jdTag_p1_topLeft set Some(MCoords2di(45, 40))
              }
              .toTree,
            JdTag.edgeQdTree(2, coord1)
          )
        )
      )
    )
    _writeReadMatchTest(doc)
  }

}
