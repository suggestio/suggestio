package io.suggest.sjs.common.fsm.signals

import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.16 15:39
  * Description: Файл с разными очень базовыми сигналами между различными FSM.
  */


/** Сигнал для завершения работы FSM-получателя. */
case class Stop()
  extends IFsmMsg


/** Сигнал видимости или невидимости контента, связанного с указанным FSM. */
case class Visible(isVisible: Boolean)
  extends IFsmMsg


/** Сигнал о готовности внешнего девайса для исполнения кода системы.
  * Сигнал появился впервые на фоне необходимости взаимодействия выдачи с cordova. */
case class CordovaDeviceReady(event: Event)
  extends IFsmMsg
object CordovaDeviceReady
  extends IFsmEventMsgCompanion
