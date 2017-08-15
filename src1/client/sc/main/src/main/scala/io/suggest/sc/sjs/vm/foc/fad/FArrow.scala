package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.common.MHand
import io.suggest.sc.ScConstants.Focused.{ARROW_ID, ARROW_OFFSET_PX, arrowClass}
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.{StyleLeft, StyleTop}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.08.15 15:37
 * Description: VM для работы со стрелкой курсора.
 */
object FArrow extends FindDiv {

  override type T = FArrow
  override def DOM_ID = ARROW_ID

  def dirCssClass(direction: MHand): String = {
    arrowClass(direction.name)
  }

}


import FArrow.dirCssClass


/** Трейт для классов-реализаций модели. */
trait FArrowT extends VmT with StyleLeft with StyleTop {

  override type T = HTMLDivElement

  // TODO: Тут всё как-то слишком многословно. Если в protected-методах не будет нужды, то их надо спилить.
  protected def unsetDirection(direction: MHand): Unit = {
    unsetDirection( dirCssClass(direction) )
  }
  protected[this] def unsetDirection(directionClass: String): Unit = {
    removeClass(directionClass)
  }

  def setDirection(direction: MHand): this.type = {
    unsetDirection(direction.inverted)
    setDirection( dirCssClass(direction) )
    this
  }
  protected[this] def setDirection(directionClass: String): Unit = {
    addClasses(directionClass)
  }

  // TODO Наверное надо использовать will-change + translate3d. Это будет аппаратно-ускорено.
  def updateX(cursorX: Int) = setLeftPx(cursorX + ARROW_OFFSET_PX)
  def updateY(cursorY: Int) = setTopPx(cursorY + ARROW_OFFSET_PX)
}


/** Реализация модели управления стрелками. */
case class FArrow(
  override val _underlying: HTMLDivElement
)
  extends FArrowT
