package io.suggest.lk.adn.map.m

import diode.FastEq
import io.suggest.geo.MGeoPoint

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:41
  * Description: Модель состояния размещения узла на карте.
  */

object MLamS {

  /** Поддержка FastEq для [[MLamS]]. */
  implicit object MLamSFastEq extends FastEq[MLamS] {
    override def eqv(a: MLamS, b: MLamS): Boolean = {
      a.center eq b.center
    }
  }

}


/** Класс модели состояния размещения узла на карте.
  *
  * @param center Координаты маркера размещения узла.
  */
case class MLamS(
                  center: MGeoPoint
                ) {

  def withGeoPoint(center2: MGeoPoint) = copy(center = center2)

}
