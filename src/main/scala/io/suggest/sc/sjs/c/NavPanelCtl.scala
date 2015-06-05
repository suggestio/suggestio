package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.{GridOffsetSetter, CtlT}
import io.suggest.sc.sjs.m.mnav.MNavDom
import io.suggest.sc.sjs.v.nav.NavPaneView

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


  /** Трейт для быстрой сборки считалки-обновлялки grid offsets. */
  trait GridOffsetCalc extends super.GridOffsetCalc {
    override def elOpt    = MNavDom.rootDiv
    override def minWidth = 280
    override def setOffset(newOff: Int): Unit = {
      mgs.leftOffset = newOff
    }
  }

  /** Экшен отображения панели на экран. */
  def showPanel(): Unit = {
    ???
  }

  /** Экшен сокрытия отображаемой панели. */
  def hidePanel(): Unit = {
    ???
  }

}
