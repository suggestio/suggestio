package io.suggest.sc.sjs.m.mfsm

import org.scalajs.dom.{Event, EventTarget}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.06.15 14:51
  * Description: Интерфейс сообщений и common-сообщения для посылания в focused fsm.
  */
trait IFsmMsg


/** Интерфейс для компаньонов классов-сообщений, завящанных на event'ы. */
trait IFsmMsgCompanion[T] {
  def apply(e: T): IFsmMsg
}


/** Интерфейс для сообщений-контейнеров Event'ов. */
trait IFsmEventMsgCompanion
  extends IFsmMsgCompanion[Event]


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
