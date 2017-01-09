package io.suggest.sjs.common.model

import org.scalajs.dom._

/** Из-за асинхронной природы ScFsm, поле currentTarget может потеряться в ходе
  * асинхронной обработки сигнала после bubbling'а. */
trait CurrentTargetBackup {

  /** Доступ к экземпляру события. */
  def event: Event

  type CurrentTarget_t <: EventTarget

  /** Цель, на которую повешен event-listener. */
  protected val _currentTarget: CurrentTarget_t = {
    event.currentTarget.asInstanceOf[CurrentTarget_t]
  }

  /** Геттер для бэкапа currentTarget, который можно переопределять при необходимости.
    * @return null || EventTarget+. */
  def currentTarget = _currentTarget

  /** @return None || Some(EventTarget+) */
  def currentTargetOpt = Option(currentTarget)

}
