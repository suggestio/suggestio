package io.suggest.sc.sjs.v.grid

import io.suggest.sc.sjs.c.GridCtl
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mgrid.{MGrid, ICwCm}
import io.suggest.sc.sjs.m.mv.IVCtx
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.view.safe.evtg.SafeEventTarget
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement
import io.suggest.sc.sjs.m.mgrid.MGridDom._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:15
 * Description: view плитки рекламный карточек. Он получает команды от контроллеров или других view'ов,
 * поддерживая тем самым состояние отображаемой плитки карточек.
 */
object GridView {

  /** Подстроить размеры контейнеров сетки под параметры экрана. */
  def adjustDom()(implicit vctx: IVCtx): Unit = {
    val height  = MAgent.availableScreen.height
    val wrappers = rootDiv() ++ wrapperDiv()
    VUtil.setHeightRootWrapCont(height, containerDiv(), wrappers)
    // В оригинале тут была ещё и обработка close screen, но оно не нужно, и это должно быть в CloseView.
  }

  /**
   * Выставить новый размер контейнера сетки.
   * @param sz Данные по новому размеру.
   */
  def setContainerSz(sz: ICwCm)(implicit vctx: IVCtx): Unit = {
    val width = sz.cw.toString + "px"

    containerDiv().foreach { cont =>
      cont.style.width    = width
      cont.style.left     = (sz.cm/2).toString + "px"
      cont.style.opacity  = "1"
    }

    loaderDiv().foreach { loader =>
      loader.style.width  = width
    }
  }


  /** Подписать страницу на события браузера. */
  def attachEvents()(implicit vctx: IVCtx): Unit = {
    loadMoreOnScroll(vctx)
  }

  /** При скроллинге нужно сообщать контроллеру о необходимости подгрузки ещё карточек. */
  private def loadMoreOnScroll(vctx: IVCtx): Unit = {
    for {
      _wrapper <- wrapperDiv()
      _content <- contentDiv()
    } {
      SafeEventTarget(_wrapper).addEventListener("scroll") { (evt: Event) =>
        handleScrollEvent(_wrapper, _content, evt)
      }
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
