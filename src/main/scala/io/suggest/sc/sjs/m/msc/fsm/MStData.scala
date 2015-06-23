package io.suggest.sc.sjs.m.msc.fsm

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgeo.{MGeoModeLoc, MGeoModeIp, IMGeoMode, MGeoLoc}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.06.15 17:33
 * Description: immutable-контейнер для передачи данных Sc FSM между состояниями.
 * @param screen Данные по экрану, если известны.
 * @param geoLoc Текущие данные по геолокации.
 */
case class MStData(
  screen    : Option[IMScreen]  = None,
  geoLoc    : Option[MGeoLoc]   = None
) {
  
 def currGeoMode: IMGeoMode = {
   geoLoc.fold[IMGeoMode](MGeoModeIp)(MGeoModeLoc.apply)
 }
  
}
