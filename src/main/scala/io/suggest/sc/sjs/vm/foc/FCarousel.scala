package io.suggest.sc.sjs.vm.foc

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.v.vutil.{ExtraStyles, VUtil}
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.css.{StyleLeft, Width}
import io.suggest.sjs.common.view.vutil.CssSzImplicits
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

  override def DOM_ID: String = "smFocCar"
  override type T = FCarousel

  /**
   * Пересоздать карусель БЕЗ привязки к DOM.
   * @return Экземпляр новосозданной карусели.
   */
  def apply(): FCarousel = {
    val el = VUtil.newDiv()
    FCarousel(el)
  }

}


/** Логика работы карусели живёт в этом трейте. */
trait FCarouselT extends SafeElT with CssSzImplicits with Width with ExtraStyles with StyleLeft {

  override type T = HTMLDivElement

  /** Выставить новую ширину карусели. */
  override def setWidthPx(widthPx: Int) = super.setWidthPx(widthPx)

  /** Прицепить ячейку справа. */
  def pushCellRight(cell: FCarCellT): Unit = {
    _underlying.appendChild(cell._underlying)
  }

  /** Прицепить ячейку слева. */
  def pushCellLeft(cell: FCarCellT): Unit = {
    _underlying.insertBefore(cell._underlying, _underlying.firstChild)
  }

  /** Мгновенное неанимированное репозиционирование на указанный сдвиг в пикселях. */
  override def setLeftPx(leftPx: Int) = super.setLeftPx(leftPx)

  /** Активация will-change при подготовке к анимации. */
  def willAnimate(enabled: Boolean): Unit = {
    _underlying.style.willChange = if (enabled) "translate3d" else ""
  }

  /** Анимированный слайдинг на указанную X-координату. */
  def animateToX(xPx: Int): Unit = {
    _underlying.style.transform = "translate3d(" + xPx.px + ",0px,0px)"
  }

  /** Итератор уже имеющихся ячеек карусели. */
  def cellsIter: Iterator[FCarCell] = {
    DomListIterator( _underlying.children )
      .map { v => FCarCell( v.asInstanceOf[HTMLDivElement] ) }
  }

}


/** Дефолтовая реалиция vm карусели. */
case class FCarousel(
  override val _underlying: HTMLDivElement
)
  extends FCarouselT
