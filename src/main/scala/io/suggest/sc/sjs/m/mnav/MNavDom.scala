package io.suggest.sc.sjs.m.mnav

import io.suggest.sc.ScConstants._, NavPane._
import io.suggest.sc.sjs.vm.util.domget.GetDivById
import org.scalajs.dom.raw.{NodeList, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 11:25
 * Description: Доступ к DOM-элементам, связанным с панелью навигации.
 */
trait MNavDomT extends GetDivById {

  def SCREEN_OFFSET     = 129
  def GNL_DOM_HEIGHT    = 44

  def rootDiv           = getDivById(ROOT_ID)

  def nodeListDiv       = getDivById(NODE_LIST_ID)

  def wrapperDiv        = getDivById(WRAPPER_ID)

  def contentDiv        = getDivById(CONTENT_ID)

  def showPanelBtn      = getDivById(SHOW_PANEL_BTN_ID)

  def gnContainerDiv    = getDivById(GN_CONTAINER_ID)

  def gnlBodyId(index: Int)       = GNL_BODY_DIV_ID_PREFIX + index
  def gnlBody(index: Int)         = getDivById( gnlBodyId(index) )

  // wrapper внутри body
  def gnlWrapperId(index: Int)    = gnlBodyId(index) + DIV_WRAPPER_SUFFIX
  def gnlWrapper(index: Int)      = getDivById( gnlWrapperId(index) )

  // content внутри wrapper
  def gnlContentId(index: Int)    = gnlBodyId(index) + DIV_CONTENT_SUFFIX
  def gnlContent(index: Int)      = getDivById( gnlContentId(index) )

  def gnlCaptionId(index: Int)    = GNL_CAPTION_DIV_ID_PREFIX + index
  def gnlCaptionDiv(index: Int)   = getDivById( gnlCaptionId(index) )

  def allGnlBodies(contentDiv: HTMLDivElement): NodeList = {
    contentDiv.getElementsByClassName(GNL_BODY_CSS_CLASS)
  }

}


object MNavDom extends MNavDomT
