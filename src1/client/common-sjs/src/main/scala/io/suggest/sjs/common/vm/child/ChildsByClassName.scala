package io.suggest.sjs.common.vm.child

import io.suggest.sjs.common.vm.Vm
import io.suggest.sjs.common.vm.of.OfHtmlElement
import org.scalajs.dom.raw.HTMLElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.16 19:13
  * Description: Поддержка поиска любых дочерних элементов по css-классу.
  */

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
