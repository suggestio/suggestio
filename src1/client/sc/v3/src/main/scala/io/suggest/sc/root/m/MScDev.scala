package io.suggest.sc.root.m

import diode.FastEq
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
        (a.geoLoc ===* b.geoLoc)
    }
  }

  implicit def univEq: UnivEq[MScDev] = UnivEq.derive

}


/** Класс модели состояния компонентов оборудования, доступного выдаче.
  *
  * @param screen Состояние экрана устройства.
  * @param geoLoc Состояния геолокации.
  */
case class MScDev(
                   screen       : MScScreenS,
                   geoLoc       : MScGeoLoc        = MScGeoLoc.empty
                 ) {

  def withScreen(screen: MScScreenS)      = copy(screen = screen)
  def withGeoLoc(geo: MScGeoLoc)          = copy(geoLoc = geo)

}
