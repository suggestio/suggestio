package io.suggest.sc.sjs.m.msrv

import io.suggest.sc.sjs.util.router.srv.{routes => _routes}

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 17:07
 * Description: API доступа к данным и системам связи с backend'ом, т.е. с сервером suggest.io.
 */

trait IAppStateSrv {

  /** Открытый канал до сервера s.io, если есть.
    * Если None, то компоненты, работающие через канал, должны отрабатывать запросы по http, если возможно. */
  // TODO Внутри Option должен быть фасад, скрывающий за собой конкретную реализацию системы быстрой связи с сервером.
  //def wsOpt: Option[WebSocket]

  /** Экземпляр http-роутера запросов к серверу. В отличие от статического routes, который может оказаться undefined,
    * этот динамический роутер гарантированно работает, и следует юзать именно его. */
  def routes: _routes.type

  /** Версия API backend-сервера. Записывается в запросы к sio-серверу, везде где это возможно. */
  def apiVsn: Int = 2

  /**
   * Понятие "поколения" выдачи было введено для организации псевдослучайной сортировки результатов
   * поисковых запросов на стороне сервера с возможностью постраничного вывода.
   */
  def generation: Long

}


/** Дефолтовая реализация [[IAppStateSrv]]. */
case class MAppStateSrv(
  override val routes      : _routes.type,
  override val generation  : Long = (js.Math.random() * 1000000000).toLong
)
  extends IAppStateSrv
