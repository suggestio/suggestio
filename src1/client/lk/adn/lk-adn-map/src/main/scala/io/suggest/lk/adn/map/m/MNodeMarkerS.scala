package io.suggest.lk.adn.map.m

import diode.FastEq
import io.suggest.geo.MGeoPoint

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:41
  * Description: Модель состояния размещения узла на карте.
  */

object MNodeMarkerS {

  /** Поддержка FastEq для [[MNodeMarkerS]]. */
  implicit object MNodeMarkerFastEq extends FastEq[MNodeMarkerS] {
    override def eqv(a: MNodeMarkerS, b: MNodeMarkerS): Boolean = {
      a.center eq b.center
    }
  }

}


/** Класс модели состояния размещения узла на карте.
  *
  * @param center Координаты маркера размещения узла.
  */
case class MNodeMarkerS(
                         center: MGeoPoint
                       ) {

  def withCenter(center2: MGeoPoint) = copy(center = center2)

}
