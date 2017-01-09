package io.suggest.sjs.common.vm.of

import io.suggest.sjs.common.view.VUtil.newDiv
import io.suggest.sjs.common.vm.VmT
import minitest._
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLSpanElement, HTMLDivElement, HTMLElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.16 12:45
 * Description: Тесты для [[OfHtmlElement]].
 */
object OfHtmlElementSpec extends SimpleTestSuite {

  private val ID = "_ofDiv"

  /** Создаём vm'ку для тестов. */
  private object _OfDiv extends OfHtmlElement {
    override type T = _OfDiv
    override type Dom_t = HTMLDivElement
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


  test("ofHtmlEl(valid DIV) => Some()") {
    val el = _validDiv()
    assertEquals(_OfDiv.ofHtmlEl(el) , Some(_OfDiv(el)))
  }

  test("ofHtmlEl(unexpected DIV) => None") {
    val el = newDiv()
    assertEquals( _OfDiv.ofHtmlEl(el), None )
  }

  test("ofHtmlEl(null) => None") {
    assertEquals( _OfDiv.ofHtmlEl(null), None )
  }

  test("ofHtmlEl(invalid span) => None") {
    val el = dom.document.createElement("span")
      .asInstanceOf[HTMLSpanElement]
    el.id = ID
    assertEquals( _OfDiv.ofHtmlEl(el), None )
    el.id = ""
    assertEquals( _OfDiv.ofHtmlEl(el), None )
  }


  test("ofHtmlElUp(valid DIV) => Some()") {
    val el = _validDiv()
    assertEquals(_OfDiv.ofHtmlElUp(el) , Some(_OfDiv(el)))
  }

  test("ofHtmlElUp(unexpected DIV) => None") {
    assertEquals( _OfDiv.ofHtmlElUp(newDiv()),  None)
  }

  test("ofHtmlElUp(child el inside valid div) => Some()") {
    val outer = _validDiv()
    val middle = newDiv()
    val inner = newDiv()
    // appendChild() тут не уместен, потому что он не обновляет parentElement, который используется в ofHtmlElUp().
    middle.parentElement = outer
    inner.parentElement = middle
    assertEquals( _OfDiv.ofHtmlElUp(inner), Some(_OfDiv(outer)) )
  }

}
