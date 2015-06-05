package io.suggest.sc.sjs.v.nav

import io.suggest.sc.sjs.m.magent.{IMScreen, MAgent}
import io.suggest.sc.sjs.m.mnav.{MNavState, MNavDom, MNav}
import io.suggest.sc.sjs.v.vutil.{SetStyleDisplay, VUtil}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 15:46
 * Description: Представление панели навигации по сети suggest.io и её узлам, геолокации, и т.д.
 */
object NavPaneView extends SetStyleDisplay {

  /** Выставить высоту списка узлов согласно экрану. */
  def adjustNodeList(nodeListDivOpt: Option[HTMLDivElement] = MNavDom.nodeListDiv,
                     wrapperDivOpt: Option[HTMLDivElement]  = MNavDom.wrapperDiv,
                     contentDivOpt: Option[HTMLDivElement]  = MNavDom.contentDiv,
                     availScreen: IMScreen = MAgent.availableScreen,
                     mNavState: MNavState  = MNav.state): Unit = {

    val height = availScreen.height - mNavState.screenOffset
    val wrappers = nodeListDivOpt ++ wrapperDivOpt
    VUtil.setHeightRootWrapCont(height, contentDivOpt, wrappers)
  }


  /** Установить новый список узлов в content div. */
  def setNodeListHtml(contentDiv: HTMLDivElement, html: String): Unit = {
    contentDiv.innerHTML = html
    // TODO Повесить listener'ы событий на новый список узлов.
  }

  /** Произвести отображение панели на экран. */
  def showPanel(rootDiv: HTMLDivElement): Unit = {
    displayBlock(rootDiv)
  }

  /** Скрыть панель навигации. */
  def hidePanel(rootDiv: HTMLDivElement): Unit = {
    displayNone(rootDiv)
  }

}
