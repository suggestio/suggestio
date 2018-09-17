package io.suggest.sc.m.dev

import diode.FastEq
import io.suggest.ble.beaconer.m.MBeaconerS
import io.suggest.dev.MPlatformS
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.17 11:04
  * Description: Модель оборудования, поддерживаемого выдачей.
  * Здесь живут состояния экрана, gps-модулей, маячки и т.д.
  * Общая черта: принадлежность к какому-либо железу, с которым нужно взаимодействовать.
  */
object MScDev {

  implicit object MScDevFastEq extends FastEq[MScDev] {
    override def eqv(a: MScDev, b: MScDev): Boolean = {
      (a.screen ===* b.screen) &&
        (a.platform ===* b.platform) &&
        (a.geoLoc ===* b.geoLoc) &&
        (a.beaconer ===* b.beaconer)
    }
  }

  @inline implicit def univEq: UnivEq[MScDev] = UnivEq.derive

}


/** Класс модели состояния компонентов оборудования, доступного выдаче.
  *
  * @param screen Состояние экрана устройства.
  * @param geoLoc Состояния геолокации.
  */
case class MScDev(
                   screen       : MScScreenS,
                   platform     : MPlatformS,
                   geoLoc       : MScGeoLoc        = MScGeoLoc.empty,
                   beaconer     : MBeaconerS       = MBeaconerS.empty,
                 ) {

  def withScreen(screen: MScScreenS)      = copy(screen = screen)
  def withPlatform(platform: MPlatformS)  = copy(platform = platform)
  def withGeoLoc(geo: MScGeoLoc)          = copy(geoLoc = geo)
  def withBeaconer(beaconer: MBeaconerS)  = copy(beaconer = beaconer)

}
