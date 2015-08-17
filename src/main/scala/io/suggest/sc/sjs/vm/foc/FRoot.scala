package io.suggest.sc.sjs.vm.foc

import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.display.SetDisplayEl
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.CLASS_WILL_TRANSLATE3D
import io.suggest.sc.ScConstants.Focused.{ROOT_ID, ROOT_APPEAR_CLASS, ROOT_DISAPPEAR_CLASS, ROOT_TRANSITION_CLASS}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 15:07
 * Description: ViewModel для взаимодейстия с корневым контейнером для выдачи focused ads.
 */

object FRoot extends FindDiv {
  
  override def DOM_ID: String = ROOT_ID
  override type T = FRoot

}


trait FRootT extends SafeElT with SetDisplayEl {

  override type T = HTMLDivElement

  def controls = FControls.find()
  def carousel = FCarousel.find()

  def show(): Unit = {
    displayBlock()
  }

  def hide(): Unit = {
    displayNone()
  }

  /** Включение анимации. */
  def enableTransition(): Unit = {
    addClasses(ROOT_TRANSITION_CLASS)
  }

  /** Отключение анимации. */
  def disableTransition(): Unit = {
    removeClass(ROOT_TRANSITION_CLASS)
  }

  /** Сокрытие с анимацией или без оной. */
  def disappear(): Unit = {
    addClasses(ROOT_DISAPPEAR_CLASS)
  }

  /** Частоиспользуемое комбо из enableTransition() и disappear(), но работает быстрее. */
  def appearTransition(): Unit = {
    addClasses(ROOT_APPEAR_CLASS)
  }

  /** Подготовка к translate3d-анимации. */
  def willAnimate(): Unit = {
    addClasses(CLASS_WILL_TRANSLATE3D)
  }

  def wontAnimate(): Unit = {
    removeClass(CLASS_WILL_TRANSLATE3D)
  }

}


case class FRoot(_underlying: HTMLDivElement)
  extends FRootT
{
  override lazy val controls = super.controls
  override lazy val carousel = super.carousel
}
