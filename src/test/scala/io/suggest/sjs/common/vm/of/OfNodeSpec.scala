package io.suggest.sjs.common.vm.of

import io.suggest.sjs.common.view.VUtil.newDiv
import io.suggest.sjs.common.vm.VmT
import minitest._
import org.scalajs.dom.Node
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.16 15:38
 * Description: Тесты для трейта [[OfNodeHtmlEl]].
 */
object OfNodeSpec extends SimpleTestSuite {

  private val ID = "_ofDiv"

  /** Создаём vm'ку для тестов. */
  private object _OfDiv extends OfNodeHtmlEl {
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


  private def _validDiv(): Node = {
    val el = newDiv()
    el.id = ID
    el
  }


  test("f(valid Node) => Some") {
    val el = _validDiv()
    assertEquals( _OfDiv.ofNode(el), Some(_OfDiv(el)) )
  }

  test("f(null) => None") {
    assertEquals( _OfDiv.ofNode(null), None )
  }

  test("f(unrelated Node) => None") {
    val el = newDiv()
    assertEquals( _OfDiv.ofNode(el), None )
  }


  test("f_up(valid Node) => Some") {
    val el = _validDiv()
    assertEquals( _OfDiv.ofNodeUp(el), Some(_OfDiv(el)) )
  }

  test("f_up(valid + inner node) => Some") {
    val outer = _validDiv()
    val inner = newDiv()
    outer.appendChild(inner)
    assertEquals( _OfDiv.ofNodeUp(inner), Some(_OfDiv(outer)) )
  }

  test("f_up(null) => None") {
    assertEquals( _OfDiv.ofNodeUp(null), None )
  }

}
