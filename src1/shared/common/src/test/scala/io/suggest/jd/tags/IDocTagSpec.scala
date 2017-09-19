package io.suggest.jd.tags

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.common.geom.coord.MCoords2di
import minitest._
import play.api.libs.json.{Json, OFormat}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 21:52
  * Description: Тесты для древовидной рекурсивной модели [[IDocTag]].
  */
// Надо как-то это разрулить наверное...
object IDocTagSpec extends SimpleTestSuite {

  /** Сравнивать разные инстансы тегов напрямую нельзя, т.к. они используют eq для equals.
    * Но для тестов можно сравнивать выхлопы toString с поправкой на children, чтобы везде была однотипная коллекция. */
  private def _normForToStringEq(jdt: IDocTag): IDocTag = {
    for (t <- jdt) yield {
      if (t.children.nonEmpty && !t.children.isInstanceOf[List[IDocTag]]) {
        t.withChildren( t.children.toList )
      } else {
        t
      }
    }
  }

  /** Тестирование сериализации и десериализации переданного объекта.
    *
    * @param v Тестируемое значение.
    * @param jsonFmt play JSON formatter.
    * @tparam T Тип тестируемого значения.
    */
  private def _writeReadMatchTest[T <: IDocTag](v: T)(implicit jsonFmt: OFormat[T]): Unit = {
    val jsonStr = jsonFmt.writes(v).toString()
    val jsRes = jsonFmt.reads( Json.parse( jsonStr ) )
    assert( jsRes.isSuccess, jsRes.toString )
    val v2 = jsRes.get
    assertEquals(
      _normForToStringEq(v2).toString,
      _normForToStringEq(v).toString
    )
  }

  private val bm300x140 = BlockMeta(BlockWidths.NORMAL, BlockHeights.H140, wide = false)
  private val bm300x300 = BlockMeta(BlockWidths.NORMAL, BlockHeights.H300, wide = true)


  private def coord1 = MCoords2di(10, 20)
  private def coord2 = MCoords2di(40, 40)
  private def coord3 = MCoords2di(111, 10)


  test("JSON: Empty document") {
    val doc = IDocTag.document()
    _writeReadMatchTest( doc )
  }


  test("JSON: Simple Document( Strip(PlainPayload()) )") {
    val doc = IDocTag.document(
      IDocTag.strip( bm300x140 )(
        IDocTag.edgeQd(1, coord1)
      )
    )
    _writeReadMatchTest( doc )
  }


  test("JSON: Document with two strips, each with several children") {
    val doc = IDocTag.document(
      IDocTag.strip( bm300x140 )(
        IDocTag.edgeQd(2, coord1),
        IDocTag.edgeQd(4, coord2)
      ),
      IDocTag.strip( bm300x300 )(
        //_picture( 333 ),
        IDocTag.edgeQd(5, coord1),
        IDocTag.edgeQd(1, coord2),
        IDocTag.edgeQd(2, coord3)
      )
    )
    _writeReadMatchTest(doc)
  }


  test("JSON: 3-level document tree with inner children") {
    val doc = IDocTag.document(
      IDocTag.strip(bm300x140)(
        IDocTag.edgeQd(2, coord1)
          .updateProps1(p0 => p0.withTopLeft( Some(MCoords2di(10, 20)) ) ),
        //_picture(555),
        IDocTag.edgeQd(4, coord2)
      ),
      IDocTag.strip(bm300x300)(
        //_picture(333),
        IDocTag.edgeQd(5, coord3)
          .updateProps1(p0 => p0.withTopLeft( Some(MCoords2di(45, 40)) ) ),
        IDocTag.edgeQd(2, coord1)
      )
    )
    _writeReadMatchTest(doc)
  }


}
