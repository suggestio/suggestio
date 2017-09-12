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

  private val bm300x140 = Some( BlockMeta(BlockWidths.NORMAL, BlockHeights.H140, wide = false) )
  private val bm300x300 = Some( BlockMeta(BlockWidths.NORMAL, BlockHeights.H300, wide = true) )


  test("JSON: Empty document") {
    val doc = JsonDocument.a()()
    _writeReadMatchTest( doc )
  }


  test("JSON: Simple Document( Strip(PlainPayload()) )") {
    val doc = JsonDocument.a()(
      Strip.a( bm300x140 )(
        PlainPayload(1)
      )
    )
    _writeReadMatchTest( doc )
  }


  test("JSON: Document with two strips, each with several children") {
    val doc = JsonDocument.a()(
      Strip.a( bm300x140 )(
        PlainPayload(2),
        Picture(555),
        PlainPayload(4)
      ),
      Strip.a( bm300x300 )(
        Picture(333),
        PlainPayload(5),
        PlainPayload(1),
        PlainPayload(2)
      )
    )
    _writeReadMatchTest(doc)
  }


  test("JSON: 3-level document tree with inner children") {
    val doc = JsonDocument.a()(
      Strip.a( bm300x140 )(
        AbsPos.a( MCoords2di(10, 20) ) (
          PlainPayload(2)
        ),
        Picture(555),
        PlainPayload(4)
      ),
      Strip.a( bm300x300 )(
        Picture(333),
        AbsPos.a( MCoords2di(40, 45) )(
          PlainPayload(5),
          PlainPayload(1)
        ),
        PlainPayload(2)
      )
    )
    _writeReadMatchTest(doc)
  }


}
