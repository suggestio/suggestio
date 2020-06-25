package io.suggest.sc.m.dev

import diode.FastEq
import io.suggest.ble.beaconer.MBeaconerS
import io.suggest.dev.MPlatformS
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

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
      (a.screen   ===* b.screen) &&
      (a.platform ===* b.platform) &&
      (a.geoLoc   ===* b.geoLoc) &&
      (a.beaconer ===* b.beaconer) &&
      (a.osNotify ===* b.osNotify) &&
      (a.onLine   ===* b.onLine)
    }
  }

  @inline implicit def univEq: UnivEq[MScDev] = UnivEq.derive

  // "Линзы" для упрощённого доступа к полям нижнего уровня.
  val screen = GenLens[MScDev]( _.screen )
  val platform = GenLens[MScDev]( _.platform )
  val geoLoc = GenLens[MScDev]( _.geoLoc )
  val beaconer = GenLens[MScDev]( _.beaconer )
  val osNotify = GenLens[MScDev]( _.osNotify )
  val onLine = GenLens[MScDev]( _.onLine )

}


/** Класс модели состояния компонентов оборудования, доступного выдаче.
  *
  * @param screen Состояние экрана устройства.
  * @param geoLoc Состояния геолокации.
  * @param osNotify Состояния внешних уведомлений.
  * @param onLine Состояние соединения с инетом.
  */
case class MScDev(
                   screen       : MScScreenS,
                   platform     : MPlatformS,
                   geoLoc       : MScGeoLoc        = MScGeoLoc.empty,
                   beaconer     : MBeaconerS       = MBeaconerS.empty,
                   osNotify     : MScOsNotifyS     = MScOsNotifyS.empty,
                   onLine       : MOnLineS         = MOnLineS.empty,
                 )
