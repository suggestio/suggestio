package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.{GridOffsetSetter, CtlT}
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mgrid.{MGrid, MGridState}
import io.suggest.sc.sjs.m.mnav.MNavDom
import io.suggest.sc.sjs.m.msc.fsm.MScFsm
import io.suggest.sc.sjs.m.msrv.nodes.find.{MFindNodesArgsDflt, MFindNodesArgsEmpty, MFindNodes}
import io.suggest.sc.sjs.v.nav.NavPaneView
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.view.safe.{SafeElT, SafeEl}
import io.suggest.sc.ScConstants.NavPane._
import org.scalajs.dom.{Node, Event}
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

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
    for (contentDiv <- MNavDom.contentDiv if contentDiv.firstChild == null) {
      // Запустить запрос к серверу на тему поиска узлов
      val searchNodesArgs = new MFindNodesArgsEmpty with MFindNodesArgsDflt {}
      val fut = MFindNodes.findNodes(searchNodesArgs)

      // Повесить обработчик событий на contentDiv.
      NavPaneView.earlyInitNodeList( SafeEl(contentDiv) )

      // Когда список узлов будет получен, отрендерить его в contentDiv.
      fut onSuccess { case resp =>
        // Отрендерить ответ в div
        NavPaneView.setNodeListHtml(contentDiv, resp.nodeListHtml)
        // Найти уже раскрытый слой, выставить ему высоту, записать данные о нём в состояние.
        findAndFixGnlExpanded(contentDiv)
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

  /** Найти и починить раскрытые сервером геослои. */
  def findAndFixGnlExpanded(contentDiv: HTMLDivElement): Unit = {
    val nl = MNavDom.allGnlBodies(contentDiv)
    DomListIterator(nl)
      .map { node =>
        SafeEl( node.asInstanceOf[HTMLDivElement] )
      }
      .filter { safeGnlBody =>
        !NavPaneView.isGnlHidden(safeGnlBody)
      }
      .flatMap { safeGnlBody =>
        safeGnlBody.getIntAttributeStrict(GNL_ATTR_LAYER_ID_INDEX)
          .map { _ -> safeGnlBody }
      }
      .foreach { case (layerIndex, safeGnlBody) =>
        MScFsm.transformStateReplace(withApply = false) {
          _.copy(
            currGnlIndex = Some(layerIndex)
          )
        }
        fixHeightForGnlExpanded(layerIndex, safeGnlBody)
      }
  }


  /** Клик внутри списка узлов. Это может быть как клик на узле, так и раскрывающемся подразделе списка узлов. */
  def onNodeListClick(e: Event): Unit = {
    val clickedNode = e.target.asInstanceOf[Node] // Node максимум, т.к. клик может быть по узлам svg
    if (clickedNode != null) {
      // Тестируем на принадлежность к заголовку слоя
      val safeClickedNode = SafeEl(clickedNode)
      val adnNodeOpt = VUtil.hasCssClass(safeClickedNode, GN_NODE_CSS_CLASS)
      if (adnNodeOpt.isDefined) {
        // Юзер кликнул по узлу в списке узлов.
        val safeEl = adnNodeOpt.get
        val adnId = safeEl.getAttribute(GN_ATTR_NODE_ID).get
        MScFsm.transformState() {
          _.copy(
            rcvrAdnId       = Some(adnId),
            navPanelOpened  = false
          )
        }
        NodeCtl.nodeSwitched(adnId)

      } else {
        // Юзер кликнул по заголовку геослоя узлов.
        val captionNodeOpt = VUtil.hasCssClass(safeClickedNode, GNL_CAPTION_CSS_CLASS)
        if (captionNodeOpt.isDefined) {
          val safeEl = captionNodeOpt.get
          val index = safeEl.getIntAttributeStrict(GNL_ATTR_LAYER_ID_INDEX).get
          MScFsm.transformState() { s =>
            s.copy(
              currGnlIndex = if (s.currGnlIndex contains index) None else Some(index)
            )
          }
        }
      } // adnNode node else
    }
  }


  /** Общий код showGnl() и hideGnl() вынесен сюда. */
  protected def _withGnl(layerIndex: Int)(f: (SafeEl[HTMLDivElement], SafeEl[HTMLDivElement]) => Unit): Unit = {
    for {
      captionDiv      <- MNavDom.gnlCaptionDiv(layerIndex)
      gnlBody         <- MNavDom.gnlBody(layerIndex)
    } {
      val safeCaptionDiv = SafeEl( captionDiv )
      val safeGnlBody = SafeEl( gnlBody )
      f(safeCaptionDiv, safeGnlBody)
    }
  }

  private def isHeightNotSet(wrapperDiv: HTMLDivElement): Boolean = {
    val h = wrapperDiv.style.height
    h == null || h.isEmpty
  }

  /** Развернуть гео-слой для отображения. */
  def showGnl(layerIndex: Int): Unit = {
    _withGnl(layerIndex) { (safeCaptionDiv, safeGnlBody) =>
      NavPaneView.showGnlBody(safeGnlBody)
      NavPaneView.activateGnlCaption(safeCaptionDiv)
      // При каждом разворачивании слоя проверяем его высоту.
      fixHeightForGnlExpanded(layerIndex, safeGnlBody)
    }
  }

  /** Подогнать высоту контейнера узлов слоя под экран. */
  def fixHeightForGnlExpanded(layerIndex: Int, safeGnlBody: SafeElT { type T = HTMLDivElement }): Unit = {
    for {
      // Второй раз пересчет не требуется,если высота у враппера уже была выставлена и TODO Opt если не было поворота экрана/ресайза (код закомменчен)
      wrapperDiv      <- MNavDom.gnlWrapper(layerIndex)// if isHeightNotSet(wrapperDiv)
      contentDiv      <- MNavDom.gnlContent(layerIndex)
      gnContainerDiv  <- MNavDom.gnContainerDiv
      gnlsCount       <- SafeEl(gnContainerDiv).getIntAttributeStrict(GN_ATTR_LAYERS_COUNT)
    } {
      val domH = safeGnlBody._underlying.offsetHeight.toInt
      val maxH = MAgent.availableScreen.height - MNavDom.SCREEN_OFFSET - (gnlsCount + 1) * MNavDom.GNL_DOM_HEIGHT
      val targetH = Math.min(maxH, domH)
      var containers = List(wrapperDiv)
      if (domH > maxH)
        containers ::= safeGnlBody._underlying
      VUtil.setHeightRootWrapCont(targetH, Some(contentDiv), containers)
    }
  }

  /** Свернуть геослой из отображения. */
  def hideGnl(layerIndex: Int): Unit = {
    _withGnl(layerIndex) { (safeCaptionDiv, safeGnlBody) =>
      NavPaneView.hideGnlBody(safeGnlBody)
      NavPaneView.deactivateGnlCaption(safeCaptionDiv)
    }
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
