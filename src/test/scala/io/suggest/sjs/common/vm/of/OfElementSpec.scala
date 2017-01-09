package io.suggest.sjs.common.vm.of

import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.view.VUtil.newDiv
import minitest._
import org.scalajs.dom.raw.{HTMLElement, Element}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.16 15:09
 * Description: Тесты для методов трейта [[OfElementHtmlEl]].
 */
object OfElementSpec extends SimpleTestSuite {

  private val ID = "_ofDiv"

  /** Создаём vm'ку для тестов. */
  private object _OfDiv extends OfElementHtmlEl {
    override type T = _OfDiv
    override type Dom_t = Element
    override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
      el.tagName.equalsIgnoreCase("DIV") &&
        el.id == ID
    }
  }
  private case class _OfDiv(override val _underlying: _OfDiv.Dom_t) extends VmT {
    override type T = _OfDiv.Dom_t
  }

  private def _validDiv(): Element = {
    val el = newDiv()
    el.id = ID
    el
  }


  test("f(valid div el) => Some()") {
    val el = _validDiv()
    assertEquals( _OfDiv.ofEl(el), Some(_OfDiv(el)) )
  }

  test("f(unrelated el) => None") {
    val el = newDiv()
    assertEquals( _OfDiv.ofEl(el), None )
  }

  test("f(null) => None") {
    assertEquals( _OfDiv.ofEl(null), None )
  }

}
