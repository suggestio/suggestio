package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mgrid.{MGridState, MGridDom, MGrid}
import io.suggest.sc.sjs.m.msc.MScState
import io.suggest.sc.sjs.m.msrv.MSrv
import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAdsReqJson, MFindAds}
import io.suggest.sc.sjs.v.grid.GridView
import io.suggest.sc.sjs.v.res.CommonRes
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.Event
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
object GridCtl extends CtlT with SjsLogger {

  /**
   * Посчитать и сохранить новые размеры сетки для текущих параметров оной.
   * Обычно этот метод вызывается в ходе добавления карточек в плитку.
   */
  def resetContainerSz(): Unit = {
    // Вычислить размер.
    val sz = MGrid.getContainerSz()
    // Обновить модель сетки новыми данными, и view-контейнеры.
    GridView.setContainerSz(sz)
    MGrid.updateState(sz)
  }

  /** Кто-то решил, что нужно загрузить ещё карточек в view. */
  def needToLoadMoreAds(): Future[MFindAds] = {
    val gstate = MGrid.state
    val findAdsArgs = MFindAdsReqJson(
      receiverId = MScState.rcvrAdnId,
      generation = Some(MSrv.generation),
      screenInfo = Some(MAgent.availableScreen),
      limit      = Some(gstate.adsPerLoad),
      offset     = Some(gstate.adsLoaded)
      // TODO Состояние геолокации сюда надо бы.
    )
    val findAdsFut = MFindAds.findAds(findAdsArgs)
    findAdsFut andThen {
      case Success(resp) => GridCtl.newAdsReceived(resp)
    }
  }

  /**
   * От сервера получена новая пачка карточек для выдачи.
   * @param resp ответ сервера.
   */
  def newAdsReceived(resp: MFindAds): Unit = {
    val mads = resp.mads
    val loaderDivOpt = MGridDom.loaderDiv()
    val safeLoaderDivOpt = loaderDivOpt.map { SafeEl.apply }
    val state = MGrid.state

    if (mads.isEmpty) {
      state.fullyLoaded = true

      // Скрыть loader-индикатор, он больше не нужен ведь.
      safeLoaderDivOpt foreach { loaderDiv =>
        GridView.Loader.hide(loaderDiv)
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
      resp.css.foreach { css =>
        CommonRes.appendCss(css)
      }

      // Посчитать и сохранить кол-во загруженных карточек плитки.
      val madsSize = mads.size
      state.adsLoaded += madsSize

      // Показать либо скрыть индикатор подгрузки выдачи.
      safeLoaderDivOpt.foreach { loaderDiv =>
        val L = GridView.Loader
        if (madsSize < state.adsPerLoad) {
          L.show(loaderDiv)
        } else {
          L.hide(loaderDiv)
          state.fullyLoaded = true
        }
      }


      // TODO Отобразить все новые карточки на экране.
      ???
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
    MGrid.resetState()

    // shared-константы между кусками метода инициализации
    val wrapperDivOpt   = MGridDom.wrapperDiv()

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
          val scrollPxToGo = contentHeight - MAgent.availableScreen.height - wrappedScrollTop
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

      val height = MAgent.availableScreen.height
      val wrappers = Seq(rootDiv, wrapperDiv)
      VUtil.setHeightRootWrapCont(height, containerDivOpt, wrappers)
    }
  }



  def resetGridOffsets(): Unit = {
    // Вызвать калькулятор размеров при ребилде. Результаты записать в соотв. модели.

    ???
  }

}
