package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.CtlT
import io.suggest.sc.sjs.v.nav.NavPaneView

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 11:31
 * Description: Контроллер панели навигации: инициализация панели, реакция на события и т.д.
 */
object NavPanelCtl extends CtlT {

  /** Инициализация панели навигации, контроллера и всего остального. */
  def initNav(): Unit = {
    NavPaneView.adjustNodeList()
  }

}
