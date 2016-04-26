package io.suggest.sc.sjs.m.mgeo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.15 16:26
  * Description: State data контейнер для данных геолокации.
  *
  * @param lastGeoLoc Последние данные от геолокации, если есть.
  * @param timer id таймера геолокации, если таймер запущен.
  */
case class MGeoLocSd(
  lastGeoLoc    : Option[MGeoLoc]     = None,
  timer         : Option[Int]         = None
)
