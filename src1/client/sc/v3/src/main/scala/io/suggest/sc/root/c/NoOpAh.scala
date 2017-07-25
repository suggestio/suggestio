package io.suggest.sc.root.c

import diode.data.Pot
import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.inx.m.WcTimeOut
import io.suggest.sc.router.routes

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.07.17 12:53
  * Description: Бывает, что надо заглушить сообщения, т.к. обработка этих сообщений стала
  * неактуальной, а сообщения всё ещё идут.
  */
class NoOpAh[M](modelRW: ModelRW[M, Pot[routes.type]]) extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Если юзер активно тыкал пальцем по экрану, то таймер сокрытия мог сработать после окончания приветствия.
    case _: WcTimeOut =>
      noChange

  }

}
