package io.suggest.sc.inx.c

import io.suggest.sc.inx.m.WcTimeOut
import io.suggest.sjs.common.controller.DomQuick

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 11:17
  * Description: Общаая утиль для welcome.
  */
object WelcomeUtil {

  /** Сборка функции отложенного (по таймеру) переключения фазы приветствия.
    *
    * @param afterMs Через сколько мс переключение?
    * @param tstamp Timestamp-маркер.
    * @return Функция с сайд-эффектом.
    *         Возвращает фьючерс, исполняющийся через afterMs миллисекунд.
    */
  def timeoutF(afterMs: Double, tstamp: Long): () => Future[WcTimeOut] = {
    { () =>
      val tp = DomQuick.timeoutPromiseT( afterMs ) {
        WcTimeOut( tstamp )
      }
      tp.fut
    }
  }

}
