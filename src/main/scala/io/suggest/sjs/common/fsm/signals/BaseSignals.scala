package io.suggest.sjs.common.fsm.signals

import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom
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
case class PlatformReady(event: Event)
  extends IFsmMsg
object PlatformReady
  extends IFsmEventMsgCompanion


/** Интерфейс для сигналов об изменении отображенности на экране текущей вкладки/приложения. */
trait IVisibilityChangeSignal extends IFsmMsg {

  /** На какое именно состояние изменилась видимость текущего приложения?
    * @return true  - теперь скрыто
    *         false - теперь видимо.
    */
  def isHidden: Boolean

}

/** Изменение видимости текущей вкладки (веб-страницы). */
case class VisibilityChange(event: Event)
  extends IVisibilityChangeSignal
{
  override def isHidden = dom.document.hidden
}
object VisibilityChange
  extends IFsmEventMsgCompanion
