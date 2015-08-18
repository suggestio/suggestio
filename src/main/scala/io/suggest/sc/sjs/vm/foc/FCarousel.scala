package io.suggest.sc.sjs.vm.foc

import io.suggest.common.css.CssSzImplicits
import io.suggest.sc.sjs.v.vutil.ExtraStyles
import io.suggest.sc.sjs.vm.foc.fad.{FAdRootT, FAdRoot}
import io.suggest.sc.sjs.vm.util.{WillTranslate3d, IInitLayout, ClearT}
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.css.{StyleLeft, Width}
import io.suggest.sc.ScConstants.Focused._
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
}


/** Логика работы карусели живёт в этом трейте. */
trait FCarouselT extends SafeElT with CssSzImplicits with Width with ExtraStyles with StyleLeft with ClearT
with IInitLayout with WillTranslate3d {

  override type T = HTMLDivElement

  def isEmpty: Boolean = {
    _underlying.firstChild == null
  }

  /** Выставить новую ширину карусели. */
  override def setWidthPx(widthPx: Int) = super.setWidthPx(widthPx)

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

  /** Анимированный слайдинг на указанную X-координату. */
  def animateToX(xPx: Int): Unit = {
    _underlying.style.transform = "translate3d(" + xPx.px + ",0px,0px)"
  }

  /** Итератор уже имеющихся ячеек карусели. */
  def cellsIter: Iterator[FAdRoot] = {
    DomListIterator( _underlying.children )
      .map { v => FAdRoot( v.asInstanceOf[HTMLDivElement] ) }
  }

  override def initLayout(): Unit = {
    // TODO Повесить mouse-move, mouse-click и touch-события.
  }
}


/** Дефолтовая реалиция vm карусели. */
case class FCarousel(
  override val _underlying: HTMLDivElement
)
  extends FCarouselT
