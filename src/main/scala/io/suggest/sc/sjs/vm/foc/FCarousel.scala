package io.suggest.sc.sjs.vm.foc

import io.suggest.common.css.CssSzImplicits
import io.suggest.primo.IReset
import io.suggest.sc.sjs.c.ScFsm
import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mfoc.MouseClick
import io.suggest.sc.sjs.m.mfsm.touch.{TouchCancel, TouchEnd, TouchStart}
import io.suggest.sc.sjs.v.vutil.ExtraStyles
import io.suggest.sc.sjs.vm.foc.fad.{FAdRootT, FAdRoot}
import io.suggest.sc.sjs.vm.util._
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.util.TouchUtil
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.css.{StyleLeft, Width}
import io.suggest.sc.ScConstants.Focused._
import io.suggest.sjs.common.view.vutil.OnMouseClickT
import org.scalajs.dom.{TouchEvent, MouseEvent}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.15 15:25
 * Description: VM системы горизонтального слайда focused-карточек.
 * DIV двигается пошагово влево и вправо, динамически меняя свой размер вправо,
 * выкидывая и добавляя слева и справа карточки.
 * Так же есть небольшой слайдинг на произвольное состояние (для touch-слайдинга).
 */
object FCarousel extends FindDiv {

  override def DOM_ID = CONTAINER_ID
  override type T     = FCarousel

  /**
   * Рассчет сдвига по Х (left) для full-screen отображения только указанной карточки.
   * @param index 0, 1, 2, ...
   * @param screen Экран.
   * @return Координата в пикселях. Её можно в transform:translate* и в left подставлять.
   */
  def indexToLeftPx(index: Int, screen: IMScreen): Int = {
    -index * screen.width
  }

}


import FCarousel.indexToLeftPx


/** Логика работы карусели живёт в этом трейте. */
trait FCarouselT extends SafeElT with CssSzImplicits with Width with ExtraStyles with StyleLeft with ClearT
with IInitLayout with WillTranslate3d with OnMouseClickT with OnEventToFsmUtilT with IReset {

  override type T = HTMLDivElement

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

  /** Итератор уже имеющихся ячеек карусели. */
  def cellsIter: Iterator[FAdRoot] = {
    DomListIterator( _underlying.children )
      .map { v => FAdRoot( v.asInstanceOf[HTMLDivElement] ) }
  }

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
case class FCarousel(
  override val _underlying: HTMLDivElement
)
  extends FCarouselT
