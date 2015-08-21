package io.suggest.sc.sjs.vm.foc

import io.suggest.primo.IReset
import io.suggest.sc.sjs.vm.util.{ClearT, WillTranslate3d, IInitLayout}
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.display.{ShowHideDisplayEl, SetDisplayEl}
import org.scalajs.dom.raw.HTMLDivElement
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


trait FRootT extends SafeElT with SetDisplayEl with IInitLayout with WillTranslate3d with ShowHideDisplayEl
with ClearT with IReset {

  override type T = HTMLDivElement

  def controls = FControls.find()
  def carousel = FCarousel.find()


  /** Включение анимации. */
  def enableTransition(): Unit = {
    addClasses(ROOT_TRANSITION_CLASS)
  }

  /** Отключение анимации. */
  def disableTransition(): Unit = {
    removeClass(ROOT_TRANSITION_CLASS)
  }


  /** Сокрытие с анимацией или без оной. */
  def initialDisappear(): Unit = {
    addClasses(ROOT_DISAPPEAR_CLASS)
  }

  def removeInitialDisappear(): Unit = {
    removeClass(ROOT_DISAPPEAR_CLASS)
  }


  /** Анимация основного отображения на экран. */
  def appearTransition(): Unit = {
    addClasses(ROOT_APPEAR_CLASS)
  }

  /** Анимация сокрытия focused-выдачи за экран. */
  def disappearTransition(): Unit = {
    removeClass(ROOT_APPEAR_CLASS)
  }


  override def initLayout(): Unit = {
    val f = IInitLayout.f
    controls foreach f
    carousel foreach f
  }

  override def clear(): Unit = {
    val f = ClearT.f
    controls foreach f
    carousel foreach f
  }

  /** Сброс состояния корневого элемента на исходную. */
  override def reset(): Unit = {
    // Сброс и чистка текущего div'а.
    hide()
    wontAnimate()
    clear()
    disableTransition()
    disappearTransition()
    removeInitialDisappear()
    // Сброс подчиненных элементов.
    val rf = IReset.f
    carousel.foreach(rf)
    controls.foreach(rf)
  }
}


case class FRoot(_underlying: HTMLDivElement)
  extends FRootT
{
  override lazy val controls = super.controls
  override lazy val carousel = super.carousel
}
