package io.suggest.sc.sjs.v.grid

import io.suggest.sc.sjs.c.GridCtl
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mgrid.{MGrid, ICwCm}
import io.suggest.sc.sjs.m.mv.IVCtx
import io.suggest.sc.sjs.v.VUtil
import io.suggest.sjs.common.view.SafeEventTarget
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:15
 * Description: view плитки рекламный карточек. Он получает тычки от контроллеров или других view'ов,
 * и влияет на отображаемую плитку.
 */
object GridView {

  /** Подстроить размеры контейнеров сетки под параметры экрана. */
  def adjustDom()(implicit vctx: IVCtx): Unit = {
    val g       = vctx.grid
    val height  = MAgent.availableScreen.height
    val wrappers = g.rootDiv.get ++ g.wrapperDiv.get
    VUtil.setHeightRootWrapCont(height, g.containerDiv.get, wrappers)
    // В оригинале тут была ещё и обработка close screen, но оно не нужно, и это должно быть в CloseView.
  }

  /**
   * Выставить новый размер контейнера сетки.
   * @param sz Данные по новому размеру.
   */
  def setContainerSz(sz: ICwCm)(implicit vctx: IVCtx): Unit = {
    val width = sz.cw.toString + "px"

    val _grid = vctx.grid
    val cont = _grid.containerDiv()
    cont.style.width    = width
    cont.style.left     = (sz.cm/2).toString + "px"
    cont.style.opacity  = "1"

    val loader = _grid.loaderDiv()
    loader.style.width    = width
  }


  /** Подписать страницу на события браузера. */
  def attachEvents()(implicit vctx: IVCtx): Unit = {
    loadMoreOnScroll(vctx)
  }

  /** При скроллинге нужно сообщать контроллеру о необходимости подгрузки ещё карточек. */
  private def loadMoreOnScroll(vctx: IVCtx): Unit = {
    val g = vctx.grid
    val wrapperDiv = g.wrapperDiv()
    val contentDiv = g.contentDiv()
    SafeEventTarget(wrapperDiv).addEventListener("scroll") { (evt: Event) =>
      handleScrollEvent(wrapperDiv, contentDiv, evt)
    }
  }

  /** Реакция на события скроллинга сетки. */
  private def handleScrollEvent(wrapperDiv: HTMLElement, contentDiv: HTMLElement, evt: Event): Unit = {
    val wrappedScrollTop = wrapperDiv.scrollTop
    val contentHeight = contentDiv.offsetHeight

    val scrollPxToGo = contentHeight - MAgent.availableScreen.height - wrappedScrollTop
    if (scrollPxToGo < MGrid.params.loadModeScrollDeltaPx) {
      GridCtl.loadMoreAds()
    }
  }

}
