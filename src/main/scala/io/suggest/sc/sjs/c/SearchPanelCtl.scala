package io.suggest.sc.sjs.c

import io.suggest.sc.ScConstants.Search.Cats
import io.suggest.sc.sjs.c.cutil.{OnEscKeyUpT, GridOffsetSetter}
import io.suggest.sc.sjs.m.mgrid.{MGridState, MGrid}
import io.suggest.sc.sjs.m.mhdr.MHeaderDom
import io.suggest.sc.sjs.m.msc.fsm.{MCatMeta, MScFsm}
import io.suggest.sc.sjs.m.msearch.{MCatsTab, MSearchDom}
import io.suggest.sc.sjs.v.layout.HeaderView
import io.suggest.sc.sjs.v.search.SearchPanelView
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.{KeyboardEvent, Node, Event}
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 18:24
 * Description: Контроллер реакции на события поисковой (правой) панели.
 */
@deprecated("FSM-MVM: OnGridSearch", "2015.aug.11")
object SearchPanelCtl extends SjsLogger with GridOffsetSetter with OnEscKeyUpT {

  /** Инициализация после загрузки выдачи узла. */
  @deprecated("Use SRoot.initLayout() instead.", "2015.aug.6")
  def initNodeLayout(): Unit = {
    SearchPanelView.adjust()

    // Инициализация кнопок переключения табов поиска.
    for {
      mtab    <- MSearchDom.mtabs
      btnDiv  <- mtab.tabBtnDiv
    } {
      SearchPanelView.initTabBtn(mtab.idModel.ROOT_DIV_ID, SafeEl(btnDiv))
    }

    // Инициализация списка категорий.
    for (catListDiv <- MCatsTab.contentDiv) {
      SearchPanelView.initCatsList( SafeEl(catListDiv) )
    }

    FtsSearchCtl.initNodeLayout()
  }


  /** Экшен для отображения панели на экране. */
  @deprecated("Use OnPlainGridState._showSearchClick() instead", "2015.aug.6")
  def showPanel(): Unit = {
    // TODO В оригинале (coffee) здесь был вызов adjust()
    val rootDivOpt = MSearchDom.rootDiv
    for (rootDiv <- rootDivOpt) {
      // Скрыть кнопки хидера главного экрана
      for (headerDiv <- MHeaderDom.rootDiv) {
        HeaderView.showBackToIndexBtns( SafeEl(headerDiv) )
      }
      SearchPanelView.showPanel(rootDiv)
      maybeRebuildGrid(rootDivOpt, isHiddenOpt = Some(false))
      // Слушать клавиатуру, когда панель открыта
      addKeyUpListener()
    }
  }

  /** При нажатии esc надо скрывать панель и обновлять состояние соответственно. */
  override protected def onFilteredKeyUp(e: KeyboardEvent): Unit = {
    HeaderCtl.hideSearchPanelBtnClick(e)
  }

  /** Экшен сокрытия панели. */
  @deprecated("See OnGridWithSearchStateT._hideSearchClick() instead", "2015.aug.6")
  def hidePanel(): Unit = {
    val rootDivOpt = MSearchDom.rootDiv
    for (rootDiv <- rootDivOpt) {
      // Скрыть на хидере главного экрана кнопки сокрытия панели
      for (headerDiv <- MHeaderDom.rootDiv) {
        HeaderView.hideBackToIndexBtns( SafeEl(headerDiv) )
      }
      SearchPanelView.hidePanel(rootDiv)
      SearchPanelCtl.maybeRebuildGrid(rootDivOpt, isHiddenOpt = Some(true))
    }
    // Клавиатура больше не нужна.
    removeKeyUpListener()
    // Отключить fts-поиск, если был включен.
    FtsSearchCtl.resetFts()
  }


  /** Если ширина экрана позволяет, то выставить сетке новый rightOffset и отребилдить. */
  private def maybeRebuildGrid(rootDivOpt   : Option[HTMLDivElement]  = MSearchDom.rootDiv,
                               isHiddenOpt  : Option[Boolean]         = None,
                               _mgs         : MGridState              = MGrid.gridState): Unit = {
    // на мобиле выдачу не надо перекорчевывать, она остается под панелью. На экранах по-шире выдача "сдвигается".
    if (_mgs.isDesktopView) {
      // Облегченный offset-калькулятор, которому ничего толком искать не надо (всё уже найдено)
      val calc = new GridOffsetCalc {
        override def mgs = _mgs
        override def elOpt = rootDivOpt
        override def isElHidden(el: HTMLElement): Boolean = {
          isHiddenOpt getOrElse super.isElHidden(el)
        }
      }
      calc.execute()
      GridCtl.rebuild()
    }
  }

  /**
   * Клик по кнопке переключения на указанную вкладку.
   * @param id id вкладки, на которую происходит переключение.
   */
  def onTabBtnClick(id: String, e: Event): Unit = {
    for {
      mtab    <- MSearchDom.mtabs
      rootDiv <- mtab.rootDiv
      btnDiv  <- mtab.tabBtnDiv
    } {
      val btnDivSafe = SafeEl(btnDiv)
      if (mtab.idModel.ROOT_DIV_ID == id) {
        // Это целевая вкладка. Отобразить её.
        SearchPanelView.showTab(rootDiv, btnDiv = btnDivSafe)
      } else {
        // [Теперь] эта вкладка неактивна.
        SearchPanelView.hideTab(rootDiv, btnDiv = btnDivSafe)
      }
    }
  }

  /**
   * Реакция на клик по ссылке категории. Надо собрать метаданные категории и .
   * @param e Исходное событие.
   */
  def onCatLinkClick(e: Event): Unit = {
    // Найти основной div ссылки категории: он помечен классом js-cat-link.
    val clickedNode = e.target.asInstanceOf[Node] // Node максимум, т.к. клик может быть по узлам svg.
    if (clickedNode != null) {
      val safeClickedEl = SafeEl(clickedNode)
      for {
        catElSafe <- VUtil.hasCssClass(safeClickedEl, Cats.ONE_CAT_LINK_CSS_CLASS)
        catState  <- MCatMeta.fromEl(catElSafe)
      } {
        MScFsm.transformState() { curState =>
          curState.copy(
            cat = Some(catState),
            searchPanelOpened = false
          )
        }
      }
    }
  }


  /** Трейт для сборки считалок-обновлялок grid offsets для search-панели. */
  @deprecated("Use SRoot.GridOffsetter instead", "2015.aug.6")
  trait GridOffsetCalc extends super.GridOffsetCalc {
    override def elOpt    = MSearchDom.rootDiv
    override def minWidth = 300
    override def setOffset(newOff: Int): Unit = {
      mgs.rightOffset = newOff
    }
  }

}
