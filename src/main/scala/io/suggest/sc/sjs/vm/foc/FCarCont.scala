package io.suggest.sc.sjs.vm.foc

import io.suggest.common.css.CssSzImplicits
import io.suggest.primo.IReset
import io.suggest.sc.ScConstants.Focused._
import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mfoc.MouseClick
import io.suggest.sc.sjs.m.mfsm.touch.{TouchCancel, TouchEnd, TouchStart}
import io.suggest.sc.sjs.vm.foc.fad.{FAdRoot, FAdRootT}
import io.suggest.sc.sjs.vm.util._
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.util.TouchUtil
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.content.ClearT
import io.suggest.sjs.common.vm.evtg.OnMouseClickT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.of.ChildrenVms
import io.suggest.sjs.common.vm.style.{StyleLeft, StyleWidth}
import io.suggest.sjs.common.vm.util.IInitLayout
import io.suggest.sjs.common.vm.will.WillTranslate3d
import org.scalajs.dom.{MouseEvent, TouchEvent}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.15 15:25
 * Description: VM системы горизонтального слайда focused-карточек.
 * DIV двигается пошагово влево и вправо, динамически меняя свой размер вправо,
 * выкидывая и добавляя слева и справа карточки.
 * Так же есть небольшой слайдинг на произвольное состояние (для touch-слайдинга).
 */
object FCarCont extends FindDiv {

  override def DOM_ID = CONTAINER_ID
  override type T     = FCarCont

  /**
   * Рассчет сдвига по Х (left) для full-screen отображения только указанной карточки.
   *
   * @param index 0, 1, 2, ...
   * @param screen Экран.
   * @return Координата в пикселях. Её можно в transform:translate* и в left подставлять.
   */
  def indexToLeftPx(index: Int, screen: IMScreen): Int = {
    -index * screen.width
  }

}


import FCarCont.{indexToLeftPx, Dom_t}


/** Логика работы карусели живёт в этом трейте. */
trait FCarContT extends VmT with CssSzImplicits with StyleWidth with StyleLeft with ClearT
with IInitLayout with WillTranslate3d with OnMouseClickT with OnEventToScFsmUtilT with IReset
with ChildrenVms {

  override type T = Dom_t

  def isEmpty: Boolean = {
    _underlying.firstChild == null
  }

  def setCellWidth(lastIndex: Int, screen: IMScreen): Unit = {
    setWidthPx( (lastIndex + 1) * screen.width )
  }

  /** Прицепить ячейку справа. */
  def pushCellRight(cell: FAdRootT): Unit = {
    _underlying.appendChild(cell._underlying)
  }

  /** Прицепить ячейку слева. */
  def pushCellLeft(cell: FAdRootT): Unit = {
    _underlying.insertBefore(cell._underlying, _underlying.firstChild)
  }

  /** Мгновенное неанимированное репозиционирование на указанный сдвиг в пикселях. */
  override def setLeftPx(leftPx: Int) = super.setLeftPx(leftPx)

  /** Активация плавности анимации. */
  def enableTransition(): Unit = {
    addClasses(ANIMATED_CSS_CLASS)
  }

  /** Деактивация плавности анимации. */
  def disableTransition(): Unit = {
    removeClass(ANIMATED_CSS_CLASS)
  }

  /** Сброс состояния карусели. */
  override def reset(): Unit = {
    wontAnimate()
    removeAttribute("style")
  }

  /** Анимированный слайдинг на указанную X-координату. */
  def animateToX(xPx: Int, browser: IBrowser): Unit = {
    val value = "translate3d(" + xPx.px + ",0px,0px)"
    val style = _underlying.style
    for (prefix <- browser.Prefixing.transforms3d) {
      val name = prefix + "transform"
      style.setProperty(name, value)
    }
  }

  def animateToCell(index: Int, screen: IMScreen, browser: IBrowser): Unit = {
    val x = indexToLeftPx(index, screen)
    animateToX(x, browser)
  }


  override type ChildVm_t = FAdRoot
  override protected def _childVmStatic = FAdRoot

  /** Итератор уже имеющихся ячеек карусели. */
  def fadRootsIter = _childrenVms

  override def initLayout(): Unit = {
    // Вешаем mouse-события, если это не-TOUCH девайс. Иначе тыканье на touch-девайсе в focused-выдаче будет превращаться в клики.
    if (!TouchUtil.IS_TOUCH_DEVICE) {
      onClick(_sendEventF(MouseClick))
      addEventListener("mousemove")(ScFsm.onMouseMove(_: MouseEvent))
    }
    // Вешаем touch события.
    _addToFsmEventListener("touchstart",  TouchStart)
    _addToFsmEventListener("touchend",    TouchEnd)
    _addToFsmEventListener("touchcancel", TouchCancel)
    addEventListener("touchmove")(ScFsm.onTouchMove(_: TouchEvent))

  }
}


/** Дефолтовая реалиция vm карусели. */
case class FCarCont(
  override val _underlying: Dom_t
)
  extends FCarContT
