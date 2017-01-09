package io.suggest.sc.sjs.vm.hdr

import io.suggest.sc.ScConstants.Header._
import io.suggest.sc.sjs.vm.hdr.btns.{HNodeLogo, HNodePrev, HBtns, HShowIndexBtn}
import io.suggest.sc.sjs.vm.hdr.btns.nav._
import io.suggest.sc.sjs.vm.hdr.btns.search._
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.util.IInitLayout
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 16:17
 * Description: ViewModel для заголовка окна.
 */
object HRoot extends FindDiv {

  override type T = HRoot

  override def DOM_ID = ROOT_DIV_ID

}


/** Трейт экземпляра модели. */
trait HRootT extends VmT with IInitLayout {

  override type T = HTMLDivElement

  // Элементы заголовка.
  /** Кнопка отображения поисковой панели, если есть. */
  def showSearchBtn = HShowSearchBtn.find()

  /** Кнопка сокрытия поисковой панели, если есть. */
  def hideSearchBtn = HHideSearchBtn.find()

  /** Кнопка отображения index'а текущей выдачи. */
  def showIndexBtn = HShowIndexBtn.find()

  /** Кнопка возврата на предыдущий узел. */
  def prevNodeBtn  = HNodePrev.find()

  /** Кнопка отображения панели навигации. */
  def showNavBtn = HShowNavBtn.find()

  /** Кнопка сокрытия панели навигации. */
  def hideNavBtn = HHideNavBtn.find()

  /** Контейнер с основными кнопками заголовка. */
  def btnsContainer = HBtns.find()

  /** Контейнер логотипа. */
  def logo = HNodeLogo.find()


  /** Реакция view'а на открытие панели поиска: изменить набор отображаемых кнопок. */
  def showBackToIndexBtns(): Unit = {
    addClasses(INDEX_ICON_CSS_CLASS)
  }

  /** Отключить отображение back to index. */
  def hideBackToIndexBtns(): Unit = {
    removeClass(INDEX_ICON_CSS_CLASS)
  }

  /** Инициализация текущей и подчиненных ViewModel'ей. */
  override def initLayout(): Unit = {
    // Используем одну и ту же функцию инициализации для всех кнопок.
    val f = IInitLayout.f
    prevNodeBtn.foreach(f)
    logo.foreach(f)
    // Инициализация кнопок, относящихся к панели поиска.
    showSearchBtn.foreach(f)
    hideSearchBtn.foreach(f)
    showIndexBtn.foreach(f)
    // Инициализируем кнопки, относящиеся к панели навигации.
    showNavBtn.foreach(f)
    hideNavBtn.foreach(f)
  }

}


/** Дефолтовая реализация экземпляра модели div-контейнера заголовка. */
case class HRoot(
  override val _underlying: HTMLDivElement
)
  extends HRootT
