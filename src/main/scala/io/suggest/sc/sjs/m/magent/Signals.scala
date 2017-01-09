package io.suggest.sc.sjs.m.magent

import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.04.16 10:38
  * Description: Стандартные сигналы от юзер-агента.
  */


/** Общий интерфейс сигналов, говорящих об изменении размеров viewport'а. */
trait IVpSzChanged extends IFsmMsg {

  /** Разрешена ли отсрочка ресайза? */
  def delayAllowed: Boolean = true

}


/** Сигнал об изменении размеров viewport'а. Исходит от window. */
case class WndResize(event: Event)
  extends IVpSzChanged
object WndResize
  extends IFsmEventMsgCompanion


/** Сигнал об изменения ориентации экрана. */
case class OrientationChange(event: Event)
  extends IVpSzChanged
object OrientationChange
  extends IFsmEventMsgCompanion


/** Дефолтовая реализация [[IVpSzChanged]]. */
case object VpSzChanged extends IVpSzChanged {
  // Это служебный внутренний сигнал, в нём запрещена отсрочка ресайза.
  override def delayAllowed = false
}
