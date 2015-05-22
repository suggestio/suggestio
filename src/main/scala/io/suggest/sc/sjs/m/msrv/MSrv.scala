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
  var generation  : Long = (js.Math.random() * 1000000000).toLong

  /** Версия API backend-сервера. Записывается в запросы к sio-серверу, везде где это возможно. */
  def apiVsn: Int = 2

}
