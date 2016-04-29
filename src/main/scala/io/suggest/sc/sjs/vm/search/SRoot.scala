package io.suggest.sc.sjs.vm.search

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msc.fsm.IStData
import io.suggest.sc.sjs.vm.search.fts.{SInput, SInputContainer}
import io.suggest.sc.sjs.vm.search.tabs.{STabsHeader, TabRootCompanion}
import io.suggest.sc.sjs.vm.search.tabs.htag.ShtRoot
import io.suggest.sc.sjs.vm.util.GridOffsetCalc
import io.suggest.sc.ScConstants.Search.ROOT_DIV_ID
import io.suggest.sc.sjs.vm.search.tabs.geo.SGeoRoot
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.StyleDisplayT
import io.suggest.sjs.common.vm.util.IInitLayout
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
trait SRootT extends VmT with StyleDisplayT with GridOffsetCalc {

  override type T = HTMLDivElement

  /** Инициализация состояния силами FSM. */
  def initLayout(sd: IStData): Unit = {
    for (screen <- sd.screen) {
      initLayout(screen, sd.browser)
    }
    for (t <- tabs) {
      t.setIsShown( t.mtab == sd.search.currTab )
    }
  }
  private def initLayout(screen: IMScreen, browser: IBrowser): Unit = {
    // Подогнать содержимое панели под экран
    adjust(screen, browser)

    val f = IInitLayout.f

    // Инициализировать панель с кнопками табов.
    tabsHdr.foreach(f)

    // Инициализировать тела табов.
    // По всей видимости, тут tabs вместо tabsIter, чтобы инициализировать lazy val tabs.
    // TODO Выправить кривую инициализацию здесь. Чтобы по списку табов не гуглять по несколько раз.
    tabs.foreach(f)

    // Инициализировать поле текстового поиска.
    input.foreach(f)
  }


  /** Доступ к div'у заголовка табов. */
  def tabsHdr = STabsHeader.find()

  /** Модели табов. */
  def tabsVms = List[TabRootCompanion](SGeoRoot, ShtRoot)

  /** Итератор для табов с ленивым поиском. */
  def tabsIter = tabsVms.iterator.flatMap(_.find())

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
    val offset = if (tabsHdr.isEmpty) 115 else 165
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
