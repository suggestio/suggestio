package io.suggest.sc.sjs.v.nav

import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mnav.MNav
import io.suggest.sc.sjs.m.mv.IVCtx
import io.suggest.sc.sjs.v.vutil.VUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 15:46
 * Description: Представление панели навигации по сети suggest.io и её узлам, геолокации, и т.д.
 */
object NavPaneView {

  /** Выставить высоту списка узлов согласно экрану. */
  def adjustNodeList()(implicit vctx: IVCtx): Unit = {
    val height = MAgent.availableScreen.height - MNav.state.screenOffset
    val nav = vctx.nav
    val wrappers = nav.nodeListDiv.get ++ nav.wrapperDiv.get
    VUtil.setHeightRootWrapCont(height, nav.contentDiv.get, wrappers)
  }


  /**
   * Отобразить кнопку навигации по ADN suggest.io.
   * @param isShown Если false, то скрыть.
   *                Если true, то показать.
   */
  def showNavShowBtn(isShown: Boolean)(implicit vctx: IVCtx): Unit = {
    vctx.nav.showPaneBtn().style.display = {
      if (isShown) "block" else "none"
    }
  }

}
