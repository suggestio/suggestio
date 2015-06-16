package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.CtlT
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 18:02
 * Description: Контроллер работы с focused-подвыдачей: отображение раскрытых карточек и прочего.
 */
object FocusedCtl extends CtlT with SjsLogger {

  /**
   * Реакция на изменение состояния текущего offset'а в focused-выдаче.
   * @param oldOff Старый offset. None значит, что focused-выдача не была активна.
   * @param newOff Новый offset. None значит, что focused выдача отключена.
   */
  def handleFocOffChanged(oldOff: Option[Int], newOff: Option[Int]): Unit = {
    ???
  }

}
