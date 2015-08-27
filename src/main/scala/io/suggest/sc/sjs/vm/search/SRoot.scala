package io.suggest.sc.sjs.vm.search

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msc.fsm.IStData
import io.suggest.sc.sjs.vm.search.fts.{SInputContainer, SInput}
import io.suggest.sc.sjs.vm.search.tabs.{TabRootCompanion, STabsHeader}
import io.suggest.sc.sjs.vm.search.tabs.htag.ShtRoot
import io.suggest.sc.sjs.vm.util.{GridOffsetCalc, IInitLayout}
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.Search.ROOT_DIV_ID
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.display.SetDisplayEl
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 11:14
 * Description: VM'ка всей поисковой панели.
 */
object SRoot extends FindDiv {

  override type T = SRoot
  override def DOM_ID: String = ROOT_DIV_ID

}


/** Логика экземпляра модели панели поиска живёт в этом трейте. */
trait SRootT extends SafeElT with SetDisplayEl with GridOffsetCalc {

  override type T = HTMLDivElement

  /** Инициализация состояния силами FSM. */
  def initLayout(sd: IStData): Unit = {
    for (screen <- sd.screen) {
      initLayout(screen, sd.browser)
    }
  }
  def initLayout(screen: IMScreen, browser: IBrowser): Unit = {
    // Подогнать содержимое панели под экран
    adjust(screen, browser)

    val f = IInitLayout.f
    // Инициализировать панель с кнопками табов.
    tabsHdr foreach f
    // Инициализировать тела табов
    tabs foreach f
    // Инициализировать поле текстового поиска.
    input foreach f
  }

  /** Доступ к div'у заголовка табов. */
  def tabsHdr = STabsHeader.find()

  /** Модели табов. */
  def tabsVms = List[TabRootCompanion](ShtRoot)

  /** Итератор для табов с ленивым поиском. */
  def tabsIter = tabsVms.iterator.flatMap { _.find() }

  /** Все табы в виде immutable-коллекции. */
  def tabs = tabsIter.toSeq

  /** Доступ к vm поля полнотекстового поиска. */
  def input = SInput.find()

  /** Контейнер input'а fts-поля. */
  def inputContainer = SInputContainer.find()

  /** Отобразить панель. */
  def show(): Unit = {
    displayBlock()
  }

  /** Скрыть панель. */
  def hide(): Unit = {
    displayNone()
  }

  /** Подогнать параметры панели под экран. */
  def adjust(screen: IMScreen, browser: IBrowser): Unit = {
    // Если нет заголовка табов, то можно делать бОльший offset.
    val offset = if (tabsHdr.isEmpty) 100 else 150
    val tabHeight = screen.height - offset
    adjust(tabHeight, browser)
  }
  /** Выставить вычисленную высоту всем табам этой поисковой панели. */
  def adjust(tabHeight: Int, browser: IBrowser): Unit = {
    for (mtab <- tabs) {
      mtab.adjust(tabHeight, browser)
    }
  }


  // Поддержка GridOffsetter.
  override protected def gridOffsetMinWidthPx: Int = 300
  override def saveNewOffsetIntoGridState(mgs0: MGridState, newOff: Int): MGridState = {
    mgs0.copy(
      rightOffset = newOff
    )
  }

}


/** Реализация модели корневого div'а панели поиска. */
case class SRoot(
  override val _underlying: HTMLDivElement
)
  extends SRootT
{

  override lazy val tabsHdr = super.tabsHdr

  override lazy val tabs = super.tabs   // этот lazy val скорее обязателен, чем желателен.

}
