package io.suggest.sc.sjs.vm.search

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.msc.fsm.IStData
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.search.tabs.{TabCompanion, STabsHeader}
import io.suggest.sc.sjs.vm.search.tabs.htag.ShtRoot
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.Search.ROOT_DIV_ID
import io.suggest.sjs.common.view.safe.{ISafe, SafeElT}
import io.suggest.sjs.common.view.safe.display.SetDisplayEl
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 11:14
 * Description: VM'ка поисковой панели.
 */
object SRoot extends FindDiv {

  override type T = SRoot

  override def DOM_ID: String = ROOT_DIV_ID

}


/** Логика экземпляра модели панели поиска живёт в этом трейте. */
trait SRootT extends SafeElT with SetDisplayEl {

  override type T = HTMLDivElement

  /** Инициализация состояния силами FSM. */
  def initLayout(stData: IStData): Unit = {
    for (screen <- stData.screen) {
      initLayout(screen)
    }
  }
  def initLayout(screen: IMScreen): Unit = {
    // Подогнать содержимое панели под экран
    adjust(screen)
    // Инициализировать панель с кнопками табов.
    for (tabHdr  <- tabsHdr) {
      tabHdr.initLayout()
    }
    ???
  }

  /** Доступ к div'у заголовка табов. */
  def tabsHdr = STabsHeader.find()

  /** Модели табов. */
  def tabsVms = List[TabCompanion](ShtRoot)

  def tabsIter = tabsVms.iterator.flatMap { _.find() }
  def tabs = tabsIter.toSeq

  /** Отобразить панель. */
  def show(): Unit = {
    displayNone()
  }

  /** Сокрыть панель. */
  def hide(): Unit = {
    displayBlock()
  }

  /** Подогнать параметры панели под экран. */
  def adjust(screen: IMScreen): Unit = {
    // Если нет заголовка табов, то можно делать бОльший offset.
    val offset = if (tabsHdr.isEmpty) 100 else 150
    val tabHeight = screen.height - offset

    // Выставить вычисленную высоту всем табам этой поисковой панели.
    // Кешируем анонимную фунцкию экстракции underlying-тегов между несколькими вызовами.
    val underF = ISafe.extractorF[HTMLElement]
    for (mtab <- tabs) {
      val tabWrapper = mtab.wrapper
      val containerOpt = tabWrapper
        .flatMap(_.content)
        .map(underF)

      val wrappersIter = (this :: tabWrapper.toList)
        .iterator
        .map(underF)

      VUtil.setHeightRootWrapCont(tabHeight, containerOpt, wrappersIter)
    }

    ???
  }

}


/** Реализация модели корневого div'а панели поиска. */
case class SRoot(
  override val _underlying: HTMLDivElement
)
  extends SRootT
{

  override lazy val tabsHdr = super.tabsHdr
  override lazy val tabs = super.tabs

}
