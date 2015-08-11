package io.suggest.sc.sjs.v.nav

import io.suggest.sc.ScConstants
import io.suggest.sc.sjs.c.NavPanelCtl
import io.suggest.sc.sjs.m.magent.{IMScreen, MAgent}
import io.suggest.sc.sjs.m.mnav.MNavDom
import io.suggest.sc.sjs.v.vutil.{OnClick, SetStyleDisplay, VUtil}
import io.suggest.sc.ScConstants.NavPane._
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import org.scalajs.dom.raw.{Event, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 15:46
 * Description: Представление панели навигации по сети suggest.io и её узлам, геолокации, и т.д.
 */
@deprecated("See vm.nav content instead", "2015.aug.11")
object NavPaneView extends SetStyleDisplay with OnClick {

  /** Выставить высоту списка узлов согласно экрану. */
  @deprecated("NlRoot.initLayout()", "2015.aug.7")
  def adjustNodeList(nodeListDivOpt: Option[HTMLDivElement] = MNavDom.nodeListDiv,
                     wrapperDivOpt: Option[HTMLDivElement]  = MNavDom.wrapperDiv,
                     contentDivOpt: Option[HTMLDivElement]  = MNavDom.contentDiv,
                     availScreen: IMScreen = MAgent.availableScreen): Unit = {

    val height = availScreen.height - ScConstants.NavPane.SCREEN_OFFSET   // mNavState.screenOffset
    val wrappers = nodeListDivOpt ++ wrapperDivOpt
    VUtil.setHeightRootWrapCont(height, contentDivOpt, wrappers)
  }


  /** Установить новый список узлов в content div. */
  @deprecated("NlContent.setContent()", "2015.aug.7")
  def setNodeListHtml(contentDiv: HTMLDivElement, html: String): Unit = {
    contentDiv.innerHTML = html
    // TODO Повесить listener'ы событий на новый список узлов.
  }

  /** Произвести отображение панели на экран. */
  @deprecated("NRoot.show()", "2015.aug.7")
  def showPanel(rootDiv: HTMLDivElement): Unit = {
    displayBlock(rootDiv)
  }

  /** Скрыть панель навигации. */
  @deprecated("NRoot.hide()", "2015.aug.7")
  def hidePanel(rootDiv: HTMLDivElement): Unit = {
    displayNone(rootDiv)
  }

  /**
   * Раняя инициализация списка узлов. На момент вызова этого метода, список узлов ещё отсутствует,
   * поэтому "earlyInit".
   * @param contentDiv Контейнер, где будет отрендерен будущий список узлов.
   */
  @deprecated("Use NlContainer.initLayout()", "2015.aug.11")
  def earlyInitNodeList(contentDiv: SafeEventTargetT): Unit = {
    // Делегировать контейнеру обработчик кликов списка.
    onClick(contentDiv) { e: Event =>
      NavPanelCtl.onNodeListClick(e)
    }
  }

  @deprecated("GlayRoot.isHidden()", "2015.aug.10")
  def isGnlHidden(bodyDiv: SafeCssElT): Boolean = {
    bodyDiv.containsClass(GNL_BODY_HIDDEN_CSS_CLASS)
  }

  @deprecated("GlayRoot.hide()", "2015.aug.11")
  def hideGnlBody(bodyDiv: SafeCssElT): Unit = {
    bodyDiv.addClasses(GNL_BODY_HIDDEN_CSS_CLASS)
  }

  @deprecated("GlayRoot.show()", "2015.aug.11")
  def showGnlBody(bodyDiv: SafeCssElT): Unit = {
    bodyDiv.removeClass(GNL_BODY_HIDDEN_CSS_CLASS)
  }

  @deprecated("GlayCaption.activate()", "2015.aug.11")
  def activateGnlCaption(captionDiv: SafeCssElT): Unit = {
    captionDiv.addClasses(GNL_ACTIVE_CSS_CLASS)
  }
  @deprecated("GlayCaption.deactivate()", "2015.aug.11")
  def deactivateGnlCaption(captionDiv: SafeCssElT): Unit = {
    captionDiv.removeClass(GNL_ACTIVE_CSS_CLASS)
  }

}
