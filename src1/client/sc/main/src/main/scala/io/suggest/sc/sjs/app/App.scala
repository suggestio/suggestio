package io.suggest.sc.sjs.app

import io.suggest.sc.sjs.c.gloc.GeoLocFsm
import io.suggest.sc.sjs.c.plat.PlatformFsm
import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.c.search.SearchFsm
import io.suggest.sc.sjs.util.logs.{GlobalErrorHandler, ScRmeLogAppender}
import io.suggest.sjs.common.log.Logging

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 15:13
 * Description: JSApp, который будет экспортироваться наружу для возможности запуска выдачи.
 */
object App {

  /** Вся работа системы выдачи начинается прямо здесь. */
  def main(args: Array[String]): Unit = {

    // Активировать отправку логов на сервер.
    Logging.LOGGERS ::= new ScRmeLogAppender

    // Повесить перехватчик ошибок на верхнем уровне.
    GlobalErrorHandler.start()

    // Запуск прослойки между платформой (браузер, cordova, etc) и системой.
    PlatformFsm.start()

    // Запуск основного FSM выдачи.
    ScFsm.start()

    // Фоновый запуск карты
    SearchFsm.start()

    // Запуск FSM геолокации
    GeoLocFsm.start()
  }

}
