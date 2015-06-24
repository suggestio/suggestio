package io.suggest.sc.sjs.m.msrv

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 10:19
 * Description: Статическая модель для переменных и констант работы с сервером.
 */
object MSrv {

  /**
   * Понятие "поколения" выдачи было введено для организации псевдослучайной сортировки результатов
   * поисковых запросов на стороне сервера с возможностью постраничного вывода.
   */
  @deprecated("FSM-MVM: use ScFsm._stateData.generation instead.", "24.jun.2015")
  var generation: Long = (js.Math.random() * 1000000000).toLong

  /** Версия API backend-сервера. Записывается в запросы к sio-серверу, везде где это возможно. */
  def API_VSN: Int = 2

}
