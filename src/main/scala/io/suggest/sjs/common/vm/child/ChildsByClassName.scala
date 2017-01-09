package io.suggest.sjs.common.vm.child

import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.{IVm, Vm}
import io.suggest.sjs.common.vm.of.{OfHtmlElement, OfNode}
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.{Element, NodeList}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.16 19:13
  * Description: Поддержка поиска любых дочерних элементов по css-классу.
  */
trait ChildElsByClassName extends IVm {

  override type T <: Element

  /**
    * Поиск дочерних HTML-элементов по имени css-класса.
    *
    * @param className Имя css-класса.
    * @return Инстанс NodeList, пришедший из рантайма.
    */
  protected def _findElsByClass(className: String): NodeList = {
    _underlying.getElementsByClassName(className)
  }

}


/** Поддержка поиска дочерних VM'ок (в т.ч. непрямых) по css-class-name'у. */
trait ChildsByClassName extends ChildElsByClassName {

  /**
    * Поиск дочерних инстансов VM, обслуживающих
    *
    * @param model Статическая часть искомой VM'ки.
    * @tparam X Тип дочерних VM'ок.
    * @return Итератор VM'ок.
    */
  protected def _findChildsByClass[X](model: IMyCssClass with OfNode { type T <: X } ): Iterator[X] = {
    DomListIterator( _findElsByClass(model.VM_CSS_CLASS) )
      // (_): method with dependent type (n: org.scalajs.dom.Node)Option[model.T] cannot be converted to function value
      .flatMap { model.ofNodeUnsafe(_) }
  }

}


/** Интерфейс для статических частей VM'ок, все элементы которых имеют отличительный css-класс,
  * по которому можно искать связанные с VM'кой элементы в рамках некоторой области. */
trait IMyCssClass {
  /** Отличительный css-класс VM'ок. */
  def VM_CSS_CLASS: String
}


/** Связка из [[IMyCssClass]] и Of-подсистемы: предикат проверки элементов дополнен проверкой css-класса. */
trait OfMyCssClass extends IMyCssClass with OfHtmlElement {
  // TODO Спилить бы abstract отсюда...
  abstract override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) &&
      Vm(el).containsClass(VM_CSS_CLASS)
  }
}
