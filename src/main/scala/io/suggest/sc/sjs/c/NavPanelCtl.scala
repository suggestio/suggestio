package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.{GridOffsetSetter, CtlT}
import io.suggest.sc.sjs.m.mgrid.{MGridDom, MGrid, MGridState}
import io.suggest.sc.sjs.m.mnav.MNavDom
import io.suggest.sc.sjs.m.msrv.nodes.find.{MFindNodesArgsDflt, MFindNodesArgsEmpty, MFindNodes}
import io.suggest.sc.sjs.v.nav.NavPaneView
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.concurrent.Future
import scala.util.Success

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 11:31
 * Description: Контроллер панели навигации: инициализация панели, реакция на события и т.д.
 */
object NavPanelCtl extends CtlT with GridOffsetSetter {

  /** Инициализация панели навигации, контроллера и всего остального. */
  def initNav(): Unit = {
    NavPaneView.adjustNodeList()
  }


  /** Экшен показа панели на экран. */
  def showPanel(): Unit = {
    // Нужно запросить список узлов, если он ещё не получен (smGeoNodesContent пустой).
    for(contentDiv <- MNavDom.contentDiv) {
      if (contentDiv.firstChild == null) {
        MFindNodes.findNodes {
          new MFindNodesArgsEmpty with MFindNodesArgsDflt {}
        } onSuccess { case resp =>
          // Отрендерить ответ в div
          NavPaneView.setNodeListHtml(contentDiv, resp.nodeListHtml)
        }
      }
    }

    // Пора приступить к отображению остальной панели на экран. Возможный рендер списка пусть идёт в фоне.
    val rootDivOpt = MNavDom.rootDiv
    for(rootDiv <- rootDivOpt) {
      NavPaneView.showPanel(rootDiv)
    }
    maybeRebuildGrid(rootDivOpt, isHiddenOpt = Some(false))
  }

  /** Если ширина экрана позволяет, то выставить сетке новый rightOffset и отребилдить. */
  def maybeRebuildGrid(rootDivOpt   : Option[HTMLDivElement]  = MNavDom.rootDiv,
                       isHiddenOpt  : Option[Boolean]         = None,
                       _mgs         : MGridState              = MGrid.state): Unit = {
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

  /** Экшен сокрытия отображаемой панели. */
  def hidePanel(): Unit = {
    val rootDivOpt = MNavDom.rootDiv
    for (rootDiv <- rootDivOpt) {
      NavPaneView.hidePanel(rootDiv)
    }
    maybeRebuildGrid(rootDivOpt, isHiddenOpt = Some(true))
  }



  /** Трейт для быстрой сборки считалки-обновлялки grid offsets. */
  trait GridOffsetCalc extends super.GridOffsetCalc {
    override def elOpt    = MNavDom.rootDiv
    override def minWidth = 280
    override def setOffset(newOff: Int): Unit = {
      mgs.leftOffset = newOff
    }
  }

}
