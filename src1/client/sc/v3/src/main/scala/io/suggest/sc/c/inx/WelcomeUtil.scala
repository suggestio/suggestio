package io.suggest.sc.c.inx

import io.suggest.sc.m.inx.WcTimeOut
import io.suggest.sjs.common.controller.DomQuick

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 11:17
  * Description: Общаая утиль для welcome.
  */
object WelcomeUtil {

  /** Запуск таймера переключения фазы приветствия.
    *
    * @param afterMs Через сколько мс переключение?
    * @param tstamp Timestamp-маркер.
    * @return Возвращает фьючерс, исполняющийся через afterMs миллисекунд.
    */
  def timeout(afterMs: Double, tstamp: Long): Future[WcTimeOut] = {
    val tp = DomQuick.timeoutPromiseT( afterMs ) {
      WcTimeOut( tstamp )
    }
    tp.fut
  }

}
