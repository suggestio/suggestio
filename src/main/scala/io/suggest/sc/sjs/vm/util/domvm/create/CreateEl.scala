package io.suggest.sc.sjs.vm.util.domvm.create

import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.domvm.DomId
import org.scalajs.dom.Element
import org.scalajs.dom.raw.HTMLDivElement

import scala.scalajs.js.Any

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 15:30
 * Description: Аддоны для быстрой сборки методов создания новых DOM-элементов под нужды ViewModel'ей.
 */
trait CreateEl {

  /** Тип собираемого экземпляра модели. */
  type E <: Any

  /** Собрать новый элемент. */
  protected def createNewEl(): E

}


/** Сборка div'ов. */
trait Div extends CreateEl {

  override type E = HTMLDivElement

  protected def createNewEl(): E = {
    VUtil.newDiv()
  }

}


/** Добавление значения DOM_ID в собираемые элементы. */
trait Id extends CreateEl with DomId {

  override type E <: Element

  abstract override protected def createNewEl(): E = {
    val el = super.createNewEl()
    el.id = DOM_ID
    el
  }

}


/** Быстрый аддон для сборки Div-элементов. */
trait CreateDiv
  extends Div
  with Id
