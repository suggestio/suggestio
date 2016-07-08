package io.suggest.sc.sjs.m.msc

import io.suggest.sjs.common.fsm.{IFsmMsg, IFsmMsgCompanion}
import org.scalajs.dom.PopStateEvent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.07.16 18:40
  * Description: Сигналы уровня всея выдачи ScFsm.
  */

/** Сигнал, контейнер события, на тему навигации по истории браузера. */
case class PopStateSignal(event: PopStateEvent)
  extends IFsmMsg
object PopStateSignal
  extends IFsmMsgCompanion[PopStateEvent]


/** Сигнал о том, что юзер что-то меняет в данных состояния URL. */
/*
case class HashChangedSignal(event: HashChangeEvent)
  extends IFsmMsg
{
  override def toString: String = {
    getClass.getSimpleName + "(" + event.oldURL + " => " + event.newURL + ")"
  }
}
object HashChangedSignal
  extends IFsmMsgCompanion[HashChangeEvent]
*/
