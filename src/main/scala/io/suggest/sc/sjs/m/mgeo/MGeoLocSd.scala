package io.suggest.sc.sjs.m.mgeo

import org.scalajs.dom.Position

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.08.15 16:26
 * Description: State data контейнер для данных геолокации.
 * @param lastBssPos Последние данные о неточной bss-геолокации, если есть.
 * @param bssWid Watcher id для неточной bss-геолокации без GPS.
 */
case class MGeoLocSd(
  bssWid        : Option[Int] = None,
  lastBssPos    : Option[Position] = None
) {


  def currGeoMode: IMGeoMode = {
    lastBssPos
      .fold[IMGeoMode] (MGeoModeIp) (pos => MGeoModeLoc(MGeoLoc(pos)))
  }

}
