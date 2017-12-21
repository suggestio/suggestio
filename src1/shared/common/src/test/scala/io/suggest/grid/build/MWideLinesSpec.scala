package io.suggest.grid.build

import io.suggest.ad.blk.BlockHeights
import japgolly.univeq._
import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.12.17 18:05
  * Description: Тесты для модели-аккамулятора широких строк.
  */
object MWideLinesSpec extends SimpleTestSuite {

  test("Extract from empty acc should return None") {
    val mwls = MWideLines()
    assertEquals(
      mwls.extract(MWideLine(1, BlockHeights.H460)),
      None
    )
  }


  test("push one + extract one should return this one") {
    val mwls0 = MWideLines()
    val mwl0 = MWideLine(1, BlockHeights.min)

    val (mwls1, _) = mwls0.push( mwl0 )
    assert( mwls1.lines.nonEmpty )

    val extractResOpt = mwls1.extract( mwl0.copy() )
    assert( extractResOpt.nonEmpty, mwls1.toString )

    val (mwls2, mwl2) = extractResOpt.get
    assert( mwls2.lines.isEmpty, mwls2.toString )
    assertEquals( mwl2, mwl0 )
  }


  test("push/extract two intercepting elements") {
    val mwl01 = MWideLine(1, BlockHeights.H300)

    // Добавить первый элемент в аккамулятор и проверить
    val (mwls01, mwl012) = MWideLines().push( mwl01 )
    assert( mwls01.lines.nonEmpty, mwls01.toString )
    assertEquals( mwl012, mwl01 )

    // Добавить второй элемент в акк и проверить:
    val mwl02 = MWideLine(1, BlockHeights.H460)
    val (mwls02, mwl022) = mwls01.push( mwl02 )
    // Элемент mwl02 должен был измениться, т.к. он метит в туже строку, что и mwl01
    assert( mwl022 !=* mwl02, s"$mwl02 == $mwl022 :: $mwls02" )
    assertEquals( mwl022.startLine, 3 )
    assert( mwls02.lines.nonEmpty, mwls02.toString )
    assertEquals( mwls02.lines.size, 2 )

    // Извлечение в исходном порядке: первый элемент.
    val exResOpt1 = mwls02.extract( mwl01 )
    assert( exResOpt1.nonEmpty, exResOpt1.toString )
    val (mwls1, mwl11) = exResOpt1.get
    assertEquals( mwl11, mwl01 )
    assertEquals( mwls1.lines.size, 1 )
    assertEquals( mwls1.extract(mwl01), None )

    // Наконец, извлечение второго элемента:
    val exResOpt2 = mwls1.extract( mwl02 )
    assert( exResOpt2.nonEmpty )
    val (mwls2, mwl12) = exResOpt2.get
    assertEquals( mwl12, mwl022 )
    assertEquals( mwl02.height, mwl12.height )

    assert(mwls2.lines.isEmpty, mwls2.toString)
  }


  test("push/extract two non-intercepting elements") {
    val mwls00 = MWideLines()

    val mwl01 = MWideLine(1, BlockHeights.H140)

    assertEquals( mwls00.isBusy( mwl01), false )

    // Добавить первый элемент в акк:
    val (mwls01, mwl012) = mwls00.push( mwl01 )
    assertEquals( mwls01.isBusy(mwl01), true )
    assertEquals( mwls01.isBusy(mwl012), true )
    assert( mwls01.lines.nonEmpty, mwls01.toString )
    assertEquals( mwl012, mwl01 )
    assertEquals( mwls01.isBusy( mwl01.withStartLine(2)), false )
    assertEquals( mwls01.isBusy( mwl01.withStartLine(18)), false )

    // Добавить второй элемент в акк и проверить:
    val mwl02 = MWideLine(2, BlockHeights.H460)
    assertEquals( mwls01.isBusy(mwl02), false )
    val (mwls02, mwl022) = mwls01.push( mwl02 )
    assertEquals( mwls02.isBusy(mwl02), true )
    // Элемент mwl02 должен был измениться, т.к. он метит в туже строку, что и mwl01
    assert( mwl022 ==* mwl02, s"$mwl02 == $mwl022 :: $mwls02" )
    assertEquals( mwls02.lines.size, 2 )

    // Извлечение в исходном порядке: первый элемент.
    val exResOpt1 = mwls02.extract( mwl01 )
    assert( exResOpt1.nonEmpty, exResOpt1.toString )
    val (mwls1, mwl11) = exResOpt1.get
    assertEquals( mwl11, mwl01 )
    assertEquals( mwls1.lines.size, 1 )
    assertEquals( mwls1.extract(mwl01), None )

    // Извлечение второго элемента:
    val exResOpt2 = mwls1.extract( mwl02 )
    assert( exResOpt2.nonEmpty )
    val (mwls2, mwl12) = exResOpt2.get
    assertEquals( mwl12, mwl022 )
    assertEquals( mwl02, mwl12 )

    assert(mwls2.lines.isEmpty, mwls2.toString)
  }

}
