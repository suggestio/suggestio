package io.suggest.sc.sjs.c

import io.suggest.sc.ScConstants.Block
import io.suggest.sc.sjs.c.cutil.{GridOffsetSetter, CtlT}
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mgrid._
import io.suggest.sc.sjs.m.mnav.MNavDom
import io.suggest.sc.sjs.m.msc.MScState
import io.suggest.sc.sjs.m.msearch.MSearchDom
import io.suggest.sc.sjs.m.msrv.MSrv
import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAdsReqJson, MFindAds}
import io.suggest.sc.sjs.v.grid.{LoaderView, GridView}
import io.suggest.sc.sjs.v.res.CommonRes
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.util.SjsLogger
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
object GridCtl extends CtlT with SjsLogger with  GridOffsetSetter {

  /**
   * Посчитать и сохранить новые размеры сетки для текущих параметров оной.
   * Обычно этот метод вызывается в ходе добавления карточек в плитку.
   */
  def resetContainerSz(containerDiv: HTMLDivElement, loaderDivOpt: Option[HTMLDivElement]): Unit = {
    // Вычислить размер.
    val sz = MGrid.getContainerSz()
    // Обновить модель сетки новыми данными, и view-контейнеры.
    GridView.setContainerSz(sz, containerDiv, loaderDivOpt)
    MGrid.updateState(sz)
  }

  def loadMoreAds(): Future[MFindAds] = {
    val gstate = MGrid.state
    val findAdsArgs = MFindAdsReqJson(
      receiverId = MScState.rcvrAdnId,
      generation = Some(MSrv.generation),
      screenInfo = Some(MAgent.availableScreen),
      limit      = Some(gstate.adsPerLoad),
      offset     = Some(gstate.adsLoaded)
      // TODO Состояние геолокации сюда надо бы.
    )
    MFindAds.findAds(findAdsArgs)
  }

  /** Кто-то решил, что нужно загрузить ещё карточек в view. */
  def needToLoadMoreAds(): Future[MFindAds] = {
    loadMoreAds() andThen {
      case Success(resp) => newAdsReceived(resp, isAdd = true)
    }
  }

  /**
   * От сервера получена новая пачка карточек для выдачи.
   * @param resp ответ сервера.
   */
  def newAdsReceived(resp: MFindAds, isAdd: Boolean): Unit = {
    val mads = resp.mads
    val loaderDivOpt = MGridDom.loaderDiv()
    val safeLoaderDivOpt = loaderDivOpt.map { SafeEl.apply }
    val state = MGrid.state

    if (mads.isEmpty) {
      state.fullyLoaded = true

      // Скрыть loader-индикатор, он больше не нужен ведь.
      for(loaderDiv <- safeLoaderDivOpt) {
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
      state.adsLoaded += madsSize

      // Показать либо скрыть индикатор подгрузки выдачи.
      safeLoaderDivOpt.foreach { loaderDiv =>
        if (madsSize < state.adsPerLoad) {
          LoaderView.show(loaderDiv)
        } else {
          LoaderView.hide(loaderDiv)
          state.fullyLoaded = true
        }
      }

      // Вызываем пересчет ширин боковых панелей в выдаче без перестройки исходной плитки.
      resetGridOffsets()

      for(containerDiv <- MGridDom.containerDiv()) {
        // Залить все карточки в DOM, создав суб-контейнер frag.
        val frag = GridView.appendNewMads(containerDiv, mads)

        // Далее логика cbca_grid.init(). Допилить сетку под новые карточки:
        resetContainerSz(containerDiv, loaderDivOpt)
        analyzeNewBlocks(frag)

        ???   // TODO Расположить все новые карточки на экране.
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
    val wrapperDivOpt = MGridDom.wrapperDiv()
    val scr = MAgent.availableScreen

    // 1. Отложенная инициализация: вешать события по мере необходимости.
    wcHideFut.onComplete { case _ =>
      val wrapperSafeOpt  = wrapperDivOpt.map { SafeEl.apply }
      val contentDivOpt   = MGridDom.contentDiv()

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
            needToLoadMoreAds()
          }
        }
      }
    }(JSExecutionContext.queue)

    val rootDivOpt = MGridDom.rootDiv()

    // 2. Выставить высоту контейнера.
    for {
      rootDiv       <- rootDivOpt
      wrapperDiv    <- wrapperDivOpt
    } {
      val containerDivOpt = MGridDom.containerDiv()

      val height = scr.height
      val wrappers = Seq(rootDiv, wrapperDiv)
      VUtil.setHeightRootWrapCont(height, containerDivOpt, wrappers)
    }
  }


  /** Перевыставить ширины боковых панелей в выдаче и боковые оффсеты в состоянии выдачи.
   *  В оригинале это была функция sm.rebuild_grid(). */
  def resetGridOffsets(): Unit = {
    // Вызвать калькулятор размеров при ребилде. Результаты записать в соотв. модели.
    val mgs = MGrid.state
    val _canNonZeroOff = mgs.canNonZeroOffset
    val wndWidth = MAgent.availableScreen.width
    lazy val _widthAdd = getWidthAdd(mgs, wndWidth)

    // Запиливаем левую панель, т.е. панель навигации.
    val navPanelSetter = new GridOffsetCalc {
      override def elOpt    = MNavDom.rootDiv()
      override def widthAdd = _widthAdd
      override def minWidth = 280
      override def canNonZeroOffset = _canNonZeroOff
      override def setOffset(newOff: Int): Unit = {
        mgs.leftOffset = newOff
      }
    }
    navPanelSetter.execute()

    // Запиливаем правую панель, т.е. панель поиска.
    val searchPanSetter = new GridOffsetCalc {
      override def elOpt    = MSearchDom.rootDiv()
      override def widthAdd = _widthAdd
      override def minWidth = 300
      override def canNonZeroOffset = _canNonZeroOff
      override def setOffset(newOff: Int): Unit = {
        mgs.rightOffset = newOff
      }
    }
    searchPanSetter.execute()
  }


  /**
   * Прочитать данные о новых блоках (отрендеренных блоках) в модель блоков.
   * В оригинале было: cbca_grid.load_blocks().
   * @param from Элемент-контейнер внутри DOM с новыми блоками.
   */
  def analyzeNewBlocks(from: Element): Unit = {
    val blockRev2 = DomListIterator(from.children)
      .foldLeft(MBlocks.blocksRev) { (acc0, e) =>
        // Пытаемся извлечь из каждого div'а необходимые аттрибуты.
        val mbiOpt = for {
          id <- VUtil.getAttribute(e, "id")
          w  <- VUtil.getIntAttribute(e, Block.BLK_WIDTH_ATTR)
          h  <- VUtil.getIntAttribute(e, Block.BLK_HEIGHT_ATTR)
        } yield {
          MBlockInfo(id = id, width = w, height = h, block = e.asInstanceOf[HTMLDivElement])
        }
        if (mbiOpt.nonEmpty) {
          mbiOpt.get :: acc0
        } else {
          warn("Unexpected element received, but block div received: " + e)
          acc0
        }
      }
    MBlocks.blocksRev = blockRev2
  }

}
