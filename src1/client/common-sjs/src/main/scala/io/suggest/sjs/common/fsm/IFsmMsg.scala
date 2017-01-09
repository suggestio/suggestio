package io.suggest.sjs.common.fsm

import io.suggest.primo.IApply1
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.06.15 14:51
  * Description: Интерфейс сообщений и common-сообщения для посылания в focused fsm.
  */
trait IFsmMsg


/** Интерфейс для компаньонов классов-сообщений, завящанных на event'ы. */
trait IFsmMsgCompanion[Arg_t] extends IApply1 {

  override type ApplyArg_t = Arg_t
  override type T = IFsmMsg

}


/** Интерфейс для сообщений-контейнеров Event'ов. */
trait IFsmEventMsgCompanion
  extends IFsmMsgCompanion[Event]
