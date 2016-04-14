package io.suggest.sc.sjs.m.mgeo

import io.suggest.sc.sjs.m.msrv.index.MNodeIndex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.08.15 16:26
 * Description: State data контейнер для данных геолокации.
 * @param lastBssPos Последние данные о неточной bss-геолокации, если есть.
 * @param bssWid Watcher id для неточной bss-геолокации без GPS.
 * @param inxReqTstamp timestamp последнего отправленного запроса за node index с геолокацией.
 * @param lastInx Последний полученный от сервера ответ по node index.
 * @param timer id таймера геолокации, если таймер запущен.
 */
case class MGeoLocSd(
  bssWid        : Option[Int]         = None,
  lastBssPos    : Option[MGeoLoc]     = None,
  inxReqTstamp  : Option[Long]        = None,
  lastInx       : Option[MNodeIndex]  = None,
  timer         : Option[Int]         = None
) {

  /** Определить и вернуть значение GeoMode для передачи на сервер.
    * Полученное значение используется для сборки запросов к серверу, связанных с геолокацией. */
  def currGeoMode: IMGeoMode = {
    lastBssPos
      .fold[IMGeoMode](MGeoModeIp)(MGeoModeLoc.apply)
  }

}
