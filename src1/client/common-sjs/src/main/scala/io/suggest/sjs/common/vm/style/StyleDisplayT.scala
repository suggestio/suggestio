package io.suggest.sjs.common.vm.style

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 11:27
 * Description: Быстрый доступ к _underlying.style.display и значениям.
 */
trait StyleDisplayT extends IVm {
  
  override type T <: HTMLElement
  
  protected def setDisplay(newDisplay: String): Unit = {
    _underlying.style.display = newDisplay
  }

  /** Скрыть из отображения текущий DOM-элемент. */
  protected def displayNone() = setDisplay("none")

  /** Отобразить текущий DOM-элемент блочно. */
  protected def displayBlock() = setDisplay("block")

  protected def isHidden: Boolean = {
    _underlying.style.display == "none"
  }
  
}


/** public-api для примитивного сокрытия/отображения элементов */
trait IShowHide {
  /** Отобразить тело текущего таба. */
  def show(): Unit
  /** Скрыть тело текущего таба. */
  def hide(): Unit
}


/** Реализация API сокрытия/отображения на основе display=block|none. */
trait ShowHideDisplayT extends StyleDisplayT with IShowHide {

  override def show(): Unit = {
    displayBlock()
  }

  override def hide(): Unit = {
    displayNone()
  }

}


/** Выставить скрытость или отображенность на основе флага. */
trait SetIsShown extends IShowHide {

  /**
   * Вызвать show или hide в зависимости от значения флага.
   * @param isShown Флаг, описывающий отображаемость элемент.
    *               true -- отображен,
    *               false -- скрыт.
   */
  def setIsShown(isShown: Boolean): Unit = {
    if (isShown) show() else hide()
  }

}
