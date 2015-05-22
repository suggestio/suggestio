package io.suggest.sc.sjs.m.magent.vsz

import minitest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 11:11
 * Description: Тестирование определения размера viewport'а.
 *
 * Для запуска теста используется RuntimeDOM, поэтому имеющиеся размеры и возможности самого API могут
 * измениться в будущем.
 *
 * [[ViewportSzTester]] используется для сборки спек тестов всей реализации и изолированных тестов
 * для каждого аддона определения размера.
 */

protected[vsz] trait ViewportSzTester extends SimpleTestSuite {

  protected def getImpl: IViewportSz

  protected def _testOne(getValF: IViewportSz => Option[Int]): Unit = {
    val impl = getImpl
    val sopt = getValF(impl)
    assert(sopt.nonEmpty)
    val s = sopt.get
    _testVal(s)
  }

  protected def _testVal(v: Int): Unit = {
    assert(v > 100)
    assert(v < 10000)
  }

  test("Width must make sense") {
    _testOne(_.widthPx)
  }

  test("Height must make sense") {
    _testOne(_.heightPx)
  }

  test("2D size must work without exceptions") {
    val impl = getImpl
    val resOpt = impl.getViewportSize
    assert( resOpt.nonEmpty )
    val res = resOpt.get
    _testVal(res.height)
    _testVal(res.width)
  }

}


/** Непосредственные тесты финальной реализации.*/
object ViewportSzSpec extends ViewportSzTester {
  override protected def getImpl: IViewportSz = {
    ViewportSz
  }
}


// Возникли проблемы с начальной задумкой: потестить каждый модуль отдельно. Почему-то RuntimeDOM не поддерживает этого.
/*
object DocElViewportSzSpec extends ViewportSzTester {
  override protected def getImpl: IViewportSz = {
    new DummyViewportSz with DocElSz {}
  }
}
object BodyElViewportSzSpec extends ViewportSzTester {
  override protected def getImpl: IViewportSz = {
    new DummyViewportSz with BodyElSz {}
  }
}
*/

/** Если нужно получить доступ к определению через аддон, то можно использовать это, чтобы запретить
  * определение размеров через стандартные window.innerWH(). */
trait DummyViewportSz extends IViewportSz {
  def widthPx: Option[Int] = None
  def heightPx: Option[Int] = None
}
