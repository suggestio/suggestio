package io.suggest.jd.tags

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.common.geom.coord.MCoords2di
import minitest._
import play.api.libs.json.{Json, OFormat}
import io.suggest.scalaz.ZTreeUtil._

import scalaz.{Show, Tree}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 21:52
  * Description: Тесты для древовидной рекурсивной модели [[IDocTag]].
  */
// Надо как-то это разрулить наверное...
object IDocTagSpec extends SimpleTestSuite {

  /** Тестирование сериализации и десериализации переданного объекта.
    *
    * @param v Тестируемое значение.
    * @param jsonFmt play JSON formatter.
    * @tparam T Тип тестируемого значения.
    */
  private def _writeReadMatchTest[T <: IDocTag](v: Tree[T])(implicit jsonFmt: OFormat[T], show: Show[T]): Unit = {
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

  private val bm300x140 = BlockMeta(BlockWidths.NORMAL, BlockHeights.H140, wide = false)
  private val bm300x300 = BlockMeta(BlockWidths.NORMAL, BlockHeights.H300, wide = true)


  private def coord1 = MCoords2di(10, 20)
  private def coord2 = MCoords2di(40, 40)
  private def coord3 = MCoords2di(111, 10)


  test("JSON: Empty document") {
    val doc = Tree.Leaf(
      IDocTag.document
    )
    _writeReadMatchTest( doc )
  }


  test("JSON: Simple Document( Strip(PlainPayload()) )") {
    val doc = Tree.Node(
      IDocTag.document,
      Stream(
        Tree.Node(
          IDocTag.strip( bm300x140 ),
          Stream(
            IDocTag.edgeQdTree(1, coord1)
          )
        )
      )
    )
    _writeReadMatchTest( doc )
  }


  test("JSON: Document with two strips, each with several children") {
    val doc = Tree.Node(
      IDocTag.document,
      Stream(
        Tree.Node(
          IDocTag.strip( bm300x140 ),
          Stream(
            IDocTag.edgeQdTree(2, coord1),
            IDocTag.edgeQdTree(4, coord2)
          )
        ),
        Tree.Node(
          IDocTag.strip( bm300x300 ),
          Stream(
            IDocTag.edgeQdTree(5, coord1),
            IDocTag.edgeQdTree(1, coord2),
            IDocTag.edgeQdTree(2, coord3)
          )
        )
      )
    )
    _writeReadMatchTest(doc)
  }


  test("JSON: 3-level document tree with inner children") {
    val doc = Tree.Node(
      IDocTag.document,
      Stream(
        Tree.Node(
          IDocTag.strip( bm300x140 ),
          Stream(
            IDocTag.edgeQdTree(2, coord1)
              .loc
              .modifyLabel { jdt =>
                jdt.withProps1(
                  jdt.props1.withTopLeft( Some(MCoords2di(10, 20)) )
                )
              }
              .toTree,
            //_picture(555),
            IDocTag.edgeQdTree(4, coord2)
          )
        ),
        Tree.Node(
          IDocTag.strip( bm300x300 ),
          Stream(
            IDocTag.edgeQdTree(5, coord3)
              .loc
              .modifyLabel { jdt =>
                jdt.withProps1(
                  jdt.props1.withTopLeft( Some(MCoords2di(45, 40)) )
                )
              }
              .toTree,
            IDocTag.edgeQdTree(2, coord1)
          )
        )
      )
    )
    _writeReadMatchTest(doc)
  }

}
