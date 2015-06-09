package io.suggest.sc.sjs.c

import io.suggest.sc.ScConstants.Block
import io.suggest.sc.sjs.c.cutil.{GridOffsetSetter, CtlT}
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mgrid._
import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAdsReqEmpty, MFindAdsReqDflt, MFindAds}
import io.suggest.sc.sjs.util.grid.builder.V1Builder
import io.suggest.sc.sjs.v.grid.{LoaderView, GridView}
import io.suggest.sc.sjs.v.res.CommonRes
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.model.browser.MBrowser
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.util.{SjsLogWrapper, SjsLogger}
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.{Element, Event}
import org.scalajs.dom.raw.HTMLDivElement
import scala.scalajs.concurrent.JSExecutionContext
import JSExecutionContext.Implicits.runNow

import scala.concurrent.Future
import scala.util.Success

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 14:22
 * Description: Контроллер сетки.
 */
object GridCtl extends CtlT with SjsLogger with GridOffsetSetter { that =>

  /**
   * Посчитать и сохранить новые размеры сетки для текущих параметров оной.
   * Обычно этот метод вызывается в ходе добавления карточек в плитку.
   */
  def resetContainerSz(containerDiv: HTMLDivElement,
                       loaderDivOpt: Option[HTMLDivElement] = MGridDom.loaderDiv): Unit = {
    // Вычислить размер.
    val sz = MGrid.getContainerSz()
    // Обновить модель сетки новыми данными, и view-контейнеры.
    GridView.setContainerSz(sz, containerDiv, loaderDivOpt)
    MGrid.state.updateWith(sz)
  }

  def askMoreAds(mgs: MGridState = MGrid.state): Future[MFindAds] = {
    val args = new MFindAdsReqEmpty with MFindAdsReqDflt {
      override def _mgs = mgs
    }
    MFindAds.findAds(args)
  }

  /** Юзер скроллит выдачу любого узла. */
  def onScroll(): Unit = {
    val mgs = MGrid.state
    if (!mgs.fullyLoaded && !mgs.isLoadingMore) {
      askMoreAds() andThen {
        case Success(resp) =>
          newAdsReceived(resp, isAdd = true)
      }
    }
  }

  /** Сброс карточек сетки. */
  def reFindAds(): Future[MFindAds] = {
    // Сброс выдачи. Должен идти перед запросом к серверу.
    MGrid.state.nothingLoaded()
    val fut = askMoreAds()
    val containerDivOpt = MGridDom.containerDiv
    // Подготовить view'ы к поступлению новых карточек
    for (containerDiv <- containerDivOpt) {
      GridView.clear(containerDiv)
    }
    // Отрендерить карточки, когда придёт ответ.
    fut.andThen { case Success(resp) =>
      newAdsReceived(resp, isAdd = false, containerDivOpt = containerDivOpt)
    }(JSExecutionContext.queue)
  }

  /**
   * От сервера получена новая пачка карточек для выдачи.
   * @param resp ответ сервера.
   */
  def newAdsReceived(resp: MFindAds, isAdd: Boolean, withAnim: Boolean = true,
                     containerDivOpt: => Option[HTMLDivElement] = MGridDom.containerDiv): Unit = {
    val mads = resp.mads
    val loaderDivOpt = MGridDom.loaderDiv
    val mgs = MGrid.state

    if (mads.isEmpty) {
      mgs.fullyLoaded = true

      // Скрыть loader-индикатор, он больше не нужен ведь.
      for(loaderDiv <- loaderDivOpt) {
        LoaderView.hide(loaderDiv)
      }

    } else {
      // Если получены новые параметры сетки, то выставить их в состояние сетки
      for {
        params2   <- resp.params
        newParams <- MGrid.params.importIfChangedFrom(params2)
      } {
        // TODO Нужно спиливать карточки, очищая сетку, если в ней уже есть какие-то карточки, отрендеренные
        // на предыдущих параметрах.
        MGrid.params = newParams
      }

      // Закачать в выдачу новый css.
      for(css <- resp.css) {
        CommonRes.appendCss(css)
      }

      // Посчитать и сохранить кол-во загруженных карточек плитки.
      val madsSize = mads.size

      // Показать либо скрыть индикатор подгрузки выдачи.
      for (loaderDiv <- loaderDivOpt) {
        if (madsSize < mgs.adsPerLoad) {
          LoaderView.hide(loaderDiv)
          mgs.fullyLoaded = true
        } else {
          LoaderView.show( SafeEl(loaderDiv) )
        }
      }

      // Вызываем пересчет ширин боковых панелей в выдаче без перестройки исходной плитки.
      resetGridOffsets()

      for (containerDiv <- containerDivOpt) {
        // Залить все карточки в DOM, создав суб-контейнер frag.
        val frag = GridView.appendNewMads(containerDiv, mads)

        // Далее логика cbca_grid.init(). Допилить сетку под новые карточки:
        resetContainerSz(containerDiv, loaderDivOpt)

        // Проанализировать залитые в DOM блоки, сохранить метаданные в модель блоков.
        val newBlocks = analyzeNewBlocks(frag)
        mgs.appendNewBlocks(newBlocks, madsSize)

        // Расположить все новые карточки на экране.
        build(isAdd, mgs, newBlocks, withAnim)

        // Вычислить максимальную высоту в колонках и расширить контейнер карточек до этой высоты.
        updateContainerHeight(containerDiv)

        // Повесить события на блоки
        GridView.initNewBlocks(newBlocks)
      }
    }
  }

  /** Сохранить вычисленное новое значение для параметра adsPerLoad в состояние grid. */
  def resetAdsPerLoad(): Unit = {
    val scr = MAgent.availableScreen
    val v = MGridState.getAdsPerLoad(scr.width)
    MGrid.state.adsPerLoad = v
  }


  /** Запрошена инициализация сетки после сброса всего layout. Такое происходит после переключения узла. */
  def initNewLayout(wcHideFut: Future[_]): Unit = {
    // shared-константы между кусками метода инициализации
    val wrapperDivOpt = MGridDom.wrapperDiv
    val scr = MAgent.availableScreen

    // 1. Отложенная инициализация: вешать события по мере необходимости.
    wcHideFut.onComplete { case _ =>
      val wrapperSafeOpt  = wrapperDivOpt.map { SafeEl.apply }
      val contentDivOpt   = MGridDom.contentDiv

      // Повесить событие реакции на скроллинг.
      for {
        wrapperSafe <- wrapperSafeOpt
        contentDiv  <- contentDivOpt
      } {
        val wrapperDiv = wrapperSafe._underlying
        // Передаем найденные элементы внутрь функции, т.к. при пересоздании layout событие будет повешено повторно.
        wrapperSafe.addEventListener("scroll") { (evt: Event) =>
          val wrappedScrollTop = wrapperDiv.scrollTop
          val contentHeight    = contentDiv.offsetHeight
          // Пнуть контроллер, чтобы подгрузил ещё карточек, когда пора.
          val scrollPxToGo = contentHeight - scr.height - wrappedScrollTop
          if (scrollPxToGo < MGrid.params.loadModeScrollDeltaPx) {
            onScroll()
          }
        }
      }
    }(JSExecutionContext.queue)

    val rootDivOpt = MGridDom.rootDiv

    // 2. Выставить высоту контейнера.
    for {
      rootDiv       <- rootDivOpt
      wrapperDiv    <- wrapperDivOpt
    } {
      val containerDivOpt = MGridDom.containerDiv

      val height = scr.height
      val wrappers = Seq(rootDiv, wrapperDiv)
      VUtil.setHeightRootWrapCont(height, containerDivOpt, wrappers)
    }
  }


  /** Перевыставить ширины боковых панелей в выдаче и боковые оффсеты в состоянии выдачи.
   *  В оригинале это была функция sm.rebuild_grid(). */
  def resetGridOffsets(): Unit = {
    // Вызвать калькулятор размеров при ребилде. Результаты записать в соотв. модели.
    val _mgs = MGrid.state
    val _canNonZeroOff = _mgs.canNonZeroOffset
    lazy val _widthAdd = getWidthAdd(_mgs)

    // Запиливаем левую панель, т.е. панель навигации.
    val navPanelSetter = new NavPanelCtl.GridOffsetCalc {
      override def mgs      = _mgs
      override def widthAdd = _widthAdd
      override def canNonZeroOffset = _canNonZeroOff
    }
    navPanelSetter.execute()

    // Запиливаем правую панель, т.е. панель поиска.
    val searchPanSetter = new SearchPanelCtl.GridOffsetCalc {
      override def mgs      = _mgs
      override def widthAdd = _widthAdd
      override def canNonZeroOffset = _canNonZeroOff
    }
    searchPanSetter.execute()
  }


  /**
   * Извлечь данные о новых блоках (отрендеренных) без сайд-эффектов.
   * В оригинале было: cbca_grid.load_blocks() с попутной заливкой в модель.
   * @param from Элемент-контейнер внутри DOM с новыми блоками.
   * @return Ленивый список инфы о новых блоках в прямом порядке.
   */
  def analyzeNewBlocks(from: Element): List[MBlockInfo] = {
    DomListIterator(from.children)
      .foldLeft(List.empty[MBlockInfo]) { (acc, e) =>
        // Пытаемся извлечь из каждого div'а необходимые аттрибуты.
        val safeEl = SafeEl(e)
        val mbiOpt = for {
          id <- safeEl.getAttribute("id")
          w  <- safeEl.getIntAttributeStrict(Block.BLK_WIDTH_ATTR)
          h  <- safeEl.getIntAttributeStrict(Block.BLK_HEIGHT_ATTR)
        } yield {
          MBlockInfo(id = id, width = w, height = h, block = e.asInstanceOf[HTMLDivElement])
        }
        if (mbiOpt.nonEmpty) {
          val mbi = mbiOpt.get
          mbi :: acc
        } else {
          warn("Unexpected element received, but block div expected: " + e)
          acc
        }
      }
      .reverse
  }

  /** Перестроить сетку. */
  def rebuild(): Unit = {
    for (containerDiv <- MGridDom.containerDiv) {
      resetContainerSz(containerDiv)
    }
    build(isAdd = false, withAnim = true)
  }

  /**
   * Построение/перестроение сетки. Здесь перепись cbca_grid.build().
   * @param isAdd true если добавление в заполненную, false если первая заливка блоков.
   * @param mgs Закешированное состояние.
   * @param addedBlocks Список добавленных блоков. В оригинале этого аргумента не было.
   * @param withAnim С анимацией? Можно её отключить принудительно. [true]
   */
  def build(isAdd: Boolean, mgs: MGridState = MGrid.state, addedBlocks: List[MBlockInfo] = Nil,
            withAnim: Boolean = true): Unit = {
    val cssPrefixes  = MBrowser.BROWSER.CssPrefixing.transforms3d

    // Собрать билдер сетки и заставить его исполнять код.
    val builder = new V1Builder with SjsLogWrapper {
      override def _LOGGER = that
      override def _isAdd = isAdd
      override def _addedBlocks = addedBlocks
      override def _mgs = mgs

      override def moveBlock(leftPx: Int, topPx: Int, b: MBlockInfo): Unit = {
        GridView.moveBlock(
          leftPx      = leftPx,
          topPx       = topPx,
          el          = b.block,
          cssPrefixes = cssPrefixes,
          withAnim    = withAnim
        )
      }
    }
    builder.execute()

    // Сохранить кое-какие черты состояния билдера в модели
    mgs.colsInfo = builder.colsInfo
  }

  /** Юзер кликает на отрендеренную карточку. */
  def onBlockClick(b: MBlockInfo, e: Event): Unit = {
    error("TODO onBlockClick " + b)
    // TODO При клике нужно или открывать новую выдачу или раскрывать focused-фунционал.
  }

  /**
   * Пересчитать высоту контейнера карточек.
   * @param colsInfo Закешированная инфа по колонкам сетки, если есть.
   * @param containerDiv Закешированный контейнер сетки, если есть.
   * @param paddedCellSize Закешированный расчет стороны ячейки, если есть.
   * @param mgp Закешированные параметры сетки, если есть.
   */
  def updateContainerHeight(containerDiv: HTMLDivElement,
                            colsInfo: Array[MColumnState] = MGrid.state.colsInfo,
                            paddedCellSize: Int = MGrid.params.paddedCellSize,
                            mgp: MGridParams = MGrid.params
                           ): Unit = {
    if (colsInfo.length > 0) {
      val maxCellH = colsInfo.iterator
        .map { _.heightUsed }
        .max
      val maxPxH = mgp.topOffset  +  paddedCellSize * maxCellH  +  mgp.bottomOffset
      GridView.setContainerHeight(maxPxH, containerDiv)

    } else {
      error("cols info empty")
    }
  }

}
