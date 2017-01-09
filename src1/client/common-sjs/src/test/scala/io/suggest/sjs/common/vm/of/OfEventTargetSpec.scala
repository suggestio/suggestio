package io.suggest.sjs.common.vm.of

import io.suggest.sjs.common.view.VUtil.newDiv
import io.suggest.sjs.common.vm.VmT
import minitest._
import org.scalajs.dom.Node
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.16 15:48
 * Description: Тесты для населения трейта [[OfEventTarget]] и [[OfEventTargetNode]].
 */
object OfEventTargetSpec extends SimpleTestSuite {

  private val ID = "_ofDiv"

  /** Создаём vm'ку для тестов. */
  private object _OfDiv extends OfEventTargetNode {
    override type T = _OfDiv
    override type Dom_t = Node
    override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
      el.tagName.equalsIgnoreCase("DIV") &&
        el.id == ID
    }
  }
  private case class _OfDiv(override val _underlying: _OfDiv.Dom_t) extends VmT {
    override type T = _OfDiv.Dom_t
  }

  /** Сгенерить валидный div. */
  private def _validDiv() = {
    val el = newDiv()
    el.id = "_ofDiv"
    el
  }


  test("f(valid evt tg) => Some") {
    val tg = _validDiv()
    assertEquals( _OfDiv.ofEventTarget(tg), Some(_OfDiv(tg)) )
  }

  test("f(null) => None") {
    assertEquals( _OfDiv.ofEventTarget(null), None )
  }

  test("f(unrelated tg) => None") {
    val tg = newDiv()
    assertEquals( _OfDiv.ofEventTarget(tg), None )
  }

}
