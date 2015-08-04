package io.suggest.sc.sjs.m.mfsm

import org.scalajs.dom.{Event, KeyboardEvent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.06.15 14:51
  * Description: Интерфейс сообщений и common-сообщения для посылания в focused fsm.
  */
trait IFsmMsg


/** Событие отжатия клавиши на клавиатуре. */
case class KbdKeyUp(e: KeyboardEvent) extends IFsmMsg


/** Интерфейс для компаньонов классов-сообщений, завящанных на event'ы. */
trait IFsmMsgCompanion[T] {
  def apply(e: T): IFsmMsg
}

/** Интерфейс для сообщений-контейнеров Event'ов. */
trait IFsmEventMsgCompanion
  extends IFsmMsgCompanion[Event]
