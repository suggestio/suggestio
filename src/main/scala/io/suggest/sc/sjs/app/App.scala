package io.suggest.sc.sjs.app

import io.suggest.sc.sjs.c.gloc.GeoLocFsm
import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.c.search.SearchFsm
import io.suggest.sc.sjs.util.logs.GlobalErrorHandler

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 15:13
 * Description: JSApp, который будет экспортироваться наружу для возможности запуска выдачи.
 */
object App extends JSApp {

  @JSExport
  override def main(): Unit = {
    // Повесить перехватчик ошибок на верхнем уровне.
    GlobalErrorHandler.start()

    // Запуск основного FSM
    ScFsm.start()

    // Фоновый запуск карты
    SearchFsm.start()

    // Запуск FSM геолокации
    GeoLocFsm.start()
  }

}
