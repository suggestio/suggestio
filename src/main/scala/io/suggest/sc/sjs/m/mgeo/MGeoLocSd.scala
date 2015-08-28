package io.suggest.sc.sjs.m.mgeo

import io.suggest.sc.sjs.m.msrv.index.{MNodeIndex, MNodeIndexTimestamped}
import org.scalajs.dom.Position

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.08.15 16:26
 * Description: State data контейнер для данных геолокации.
 * @param lastBssPos Последние данные о неточной bss-геолокации, если есть.
 * @param bssWid Watcher id для неточной bss-геолокации без GPS.
 * @param inxReqTstamp timestamp последнего отправленного запроса за node index с геолокацией.
 * @param lastInx Последний полученный от сервера ответ по node index.
 */
case class MGeoLocSd(
  bssWid        : Option[Int]         = None,
  lastBssPos    : Option[Position]    = None,
  inxReqTstamp  : Option[Long]        = None,
  lastInx       : Option[MNodeIndex]  = None
) {


  def currGeoMode: IMGeoMode = {
    lastBssPos
      .fold[IMGeoMode] (MGeoModeIp) (pos => MGeoModeLoc(MGeoLoc(pos)))
  }

}
